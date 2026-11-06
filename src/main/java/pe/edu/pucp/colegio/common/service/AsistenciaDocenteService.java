package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.config.AsistenciaProperties;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteCreateRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteResponse;
import pe.edu.pucp.colegio.common.model.AsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AsistenciaDocenteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;

/**
 * Asistencia diaria de los docentes: el administrador consulta, registra y ajusta la de cualquiera,
 * y cada docente marca su propia entrada y salida con la hora del servidor.
 */
@Service
public class AsistenciaDocenteService {

  private static final Logger log = LoggerFactory.getLogger(AsistenciaDocenteService.class);

  private final AsistenciaDocenteRepository asistencias;
  private final DocenteRepository docentes;
  private final SesionClaseRepository sesiones;
  private final AsistenciaProperties properties;
  private final DocenteService docenteService;
  private final Clock clock;

  public AsistenciaDocenteService(
      AsistenciaDocenteRepository asistencias,
      DocenteRepository docentes,
      SesionClaseRepository sesiones,
      AsistenciaProperties properties,
      DocenteService docenteService,
      Clock clock) {
    this.asistencias = asistencias;
    this.docentes = docentes;
    this.sesiones = sesiones;
    this.properties = properties;
    this.docenteService = docenteService;
    this.clock = clock;
  }

  /**
   * Registra la entrada del docente autenticado con la fecha y hora del servidor. Si tiene clases
   * ese día, solo puede marcar desde {@code ventanaDocenteAntesMinutos} antes de su primera sesión
   * y, pasada la tolerancia, la entrada queda como tardanza. Sin clases ese día, marca libremente.
   */
  @Transactional
  public AsistenciaDocenteResponse marcarEntrada(String usuario) {
    Docente docente = docenteService.obtenerDeCuenta(usuario);
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDate hoy = ahora.toLocalDate();
    LocalTime hora = ahora.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
    LocalTime inicioJornada = inicioJornada(docente.getId(), hoy);
    validarAperturaMarcaje(inicioJornada, hora);

    AsistenciaDocente asistencia =
        asistencias
            .findByDocenteIdAndFechaAndActiveTrue(docente.getId(), hoy)
            .orElseGet(() -> nuevaAsistencia(docente, hoy));
    if (asistencia.getEstado() != EstadoAsistenciaDocente.PENDIENTE_MARCAJE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La asistencia de hoy ya está registrada");
    }
    asistencia.marcarIngreso(hora);
    clasificarTardanza(asistencia, inicioJornada);
    AsistenciaDocenteResponse response = guardar(asistencia);
    log.info(
        "Docente {} marcó su entrada el {} a las {} ({})",
        docente.getId(),
        hoy,
        hora,
        response.estado());
    return response;
  }

  /** Registra la salida del docente autenticado con la hora del servidor; exige entrada previa. */
  @Transactional
  public AsistenciaDocenteResponse marcarSalida(String usuario) {
    Docente docente = docenteService.obtenerDeCuenta(usuario);
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDate hoy = ahora.toLocalDate();
    AsistenciaDocente asistencia =
        asistencias
            .findByDocenteIdAndFechaAndActiveTrue(docente.getId(), hoy)
            .filter(registrada -> registrada.getHoraEntrada() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT, "Debe marcar su entrada antes de la salida"));
    if (asistencia.getHoraSalida() != null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La salida de hoy ya está registrada");
    }
    LocalTime hora = ahora.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
    asistencia.marcarSalida(hora);
    log.info("Docente {} marcó su salida el {} a las {}", docente.getId(), hoy, hora);
    return respuesta(asistencia);
  }

  /** Lista las asistencias del docente autenticado, opcionalmente en un rango de fechas. */
  @Transactional
  public List<AsistenciaDocenteResponse> listarPropias(
      String usuario, LocalDate desde, LocalDate hasta) {
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la final");
    }
    Docente docente = docenteService.obtenerDeCuenta(usuario);
    return asistencias.buscarDelDocente(docente.getId(), desde, hasta).stream()
        .map(this::respuesta)
        .toList();
  }

  @Transactional
  public AsistenciaDocenteResponse registrar(AsistenciaDocenteCreateRequest request) {
    Docente docente = buscarDocente(request.docenteId());
    if (asistencias.existsByDocenteIdAndFechaAndActiveTrue(docente.getId(), request.fecha())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La asistencia del docente ya está registrada para esa fecha");
    }
    validarHorario(request.horaEntrada(), request.horaSalida());

    AsistenciaDocente asistencia = new AsistenciaDocente();
    asistencia.setDocente(docente);
    asistencia.setFecha(request.fecha());
    aplicarRegistro(asistencia, request);
    return guardar(asistencia);
  }

  @Transactional(readOnly = true)
  public List<AsistenciaDocenteResponse> listar(LocalDate fecha, Integer docenteId) {
    if (docenteId != null) {
      buscarDocente(docenteId);
    }
    return asistencias.buscar(fecha, docenteId).stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public AsistenciaDocenteResponse obtener(Integer id) {
    return respuesta(buscarAsistencia(id));
  }

  @Transactional
  public AsistenciaDocenteResponse ajustar(Integer id, AsistenciaDocenteAjusteRequest request) {
    AsistenciaDocente asistencia = buscarAsistencia(id);
    asistencia.ajustar(request.estado(), request.motivoAjuste());
    return respuesta(asistencia);
  }

  private void aplicarRegistro(
      AsistenciaDocente asistencia, AsistenciaDocenteCreateRequest request) {
    EstadoAsistenciaDocente estado = request.estado();
    switch (estado) {
      case PENDIENTE_MARCAJE -> {
        if (request.horaEntrada() != null || request.horaSalida() != null) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Una asistencia pendiente no puede incluir horas marcadas");
        }
      }
      case PRESENTE -> {
        exigirHoraEntrada(request.horaEntrada());
        asistencia.marcarIngreso(request.horaEntrada());
        clasificarTardanza(
            asistencia, inicioJornada(asistencia.getDocente().getId(), asistencia.getFecha()));
        if (request.horaSalida() != null) {
          asistencia.marcarSalida(request.horaSalida());
        }
      }
      case TARDANZA -> {
        exigirHoraEntrada(request.horaEntrada());
        if (request.motivoAjuste() == null || request.motivoAjuste().isBlank()) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "El motivo del ajuste es obligatorio para marcar tardanza");
        }
        asistencia.marcarIngreso(request.horaEntrada());
        asistencia.ajustar(estado, request.motivoAjuste());
        if (request.horaSalida() != null) {
          asistencia.marcarSalida(request.horaSalida());
        }
      }
      case INASISTENCIA -> {
        if (request.horaEntrada() != null || request.horaSalida() != null) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Una inasistencia no puede incluir horas marcadas");
        }
        asistencia.marcarInasistencia();
        if (request.motivoAjuste() != null && !request.motivoAjuste().isBlank()) {
          asistencia.ajustar(estado, request.motivoAjuste());
        }
      }
    }
  }

  /** Hora de inicio de la primera sesión del docente ese día, o nulo si no tiene clases. */
  private LocalTime inicioJornada(Integer docenteId, LocalDate fecha) {
    return sesiones
        .findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(docenteId, fecha)
        .map(SesionClase::getHoraInicio)
        .orElse(null);
  }

  private void clasificarTardanza(AsistenciaDocente asistencia, LocalTime inicioJornada) {
    int tolerancia = properties.getToleranciaDocenteMinutos();
    if (inicioJornada != null
        && asistencia.getHoraEntrada().isAfter(inicioJornada.plusMinutes(tolerancia))) {
      asistencia.ajustar(
          EstadoAsistenciaDocente.TARDANZA,
          "Marcaje posterior a la tolerancia de " + tolerancia + " minutos");
    }
  }

  private void validarAperturaMarcaje(LocalTime inicioJornada, LocalTime hora) {
    if (inicioJornada == null) {
      return;
    }
    LocalTime apertura = inicioJornada.minusMinutes(properties.getVentanaDocenteAntesMinutos());
    if (hora.isBefore(apertura)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El marcaje de entrada se habilita a partir de las " + apertura);
    }
  }

  private AsistenciaDocente nuevaAsistencia(Docente docente, LocalDate fecha) {
    AsistenciaDocente asistencia = new AsistenciaDocente();
    asistencia.setDocente(docente);
    asistencia.setFecha(fecha);
    return asistencia;
  }

  private AsistenciaDocenteResponse guardar(AsistenciaDocente asistencia) {
    try {
      return respuesta(asistencias.saveAndFlush(asistencia));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La asistencia del docente ya existe para esa fecha", e);
    }
  }

  private void validarHorario(java.time.LocalTime entrada, java.time.LocalTime salida) {
    if (salida != null && entrada == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Debe registrar la hora de entrada antes de la salida");
    }
    if (entrada != null && salida != null && salida.isBefore(entrada)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La salida no puede ser anterior al ingreso");
    }
  }

  private void exigirHoraEntrada(java.time.LocalTime entrada) {
    if (entrada == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La hora de entrada es obligatoria para marcar asistencia");
    }
  }

  private Docente buscarDocente(Integer id) {
    return docentes
        .findById(id)
        .filter(docente -> Boolean.TRUE.equals(docente.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el docente"));
  }

  private AsistenciaDocente buscarAsistencia(Integer id) {
    return asistencias
        .findByIdAndActiveTrue(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe la asistencia del docente"));
  }

  private AsistenciaDocenteResponse respuesta(AsistenciaDocente asistencia) {
    return new AsistenciaDocenteResponse(
        asistencia.getId(),
        asistencia.getDocente().getId(),
        asistencia.getDocente().getPersona().getNombreCompleto(),
        asistencia.getFecha(),
        asistencia.getHoraEntrada(),
        asistencia.getHoraSalida(),
        asistencia.getEstado(),
        asistencia.getMotivoAjuste());
  }
}
