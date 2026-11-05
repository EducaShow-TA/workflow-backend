package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import pe.edu.pucp.colegio.common.repository.AsistenciaDocenteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;

/** Permite consultar, registrar y ajustar la asistencia diaria de cualquier docente. */
@Service
public class AsistenciaDocenteService {

  private final AsistenciaDocenteRepository asistencias;
  private final DocenteRepository docentes;
  private final SesionClaseRepository sesiones;
  private final AsistenciaProperties properties;

  public AsistenciaDocenteService(
      AsistenciaDocenteRepository asistencias,
      DocenteRepository docentes,
      SesionClaseRepository sesiones,
      AsistenciaProperties properties) {
    this.asistencias = asistencias;
    this.docentes = docentes;
    this.sesiones = sesiones;
    this.properties = properties;
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
    try {
      return respuesta(asistencias.saveAndFlush(asistencia));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La asistencia del docente ya existe para esa fecha", e);
    }
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
        LocalTime inicioJornada =
            sesiones
                .findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(
                    asistencia.getDocente().getId(), asistencia.getFecha())
                .map(sesion -> sesion.getHoraInicio())
                .orElse(null);
        if (inicioJornada != null
            && request
                .horaEntrada()
                .isAfter(inicioJornada.plusMinutes(properties.getToleranciaDocenteMinutos()))) {
          asistencia.ajustar(
              EstadoAsistenciaDocente.TARDANZA,
              "Marcaje posterior a la tolerancia de "
                  + properties.getToleranciaDocenteMinutos()
                  + " minutos");
        }
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
