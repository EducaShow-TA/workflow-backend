package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AperturaAnhioRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoCreateRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoResponse;
import pe.edu.pucp.colegio.common.dto.VacantesPeriodoResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;

/** Gestiona los periodos académicos asociados a grado y sección. */
@Service
public class PeriodoService {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final PeriodoRepository periodos;
  private final GradoRepository grados;
  private final SeccionRepository secciones;
  private final VacantesService vacantes;
  private final CalendarioBimestralService calendario;
  private final Clock clock;

  public PeriodoService(
      PeriodoRepository periodos,
      GradoRepository grados,
      SeccionRepository secciones,
      VacantesService vacantes,
      CalendarioBimestralService calendario,
      Clock clock) {
    this.periodos = periodos;
    this.grados = grados;
    this.secciones = secciones;
    this.vacantes = vacantes;
    this.calendario = calendario;
    this.clock = clock;
  }

  @Transactional
  public PeriodoResponse crear(PeriodoCreateRequest request) {
    if (request.fechaInicio().isAfter(request.fechaFin())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha de inicio debe ser anterior o igual a la fecha de fin");
    }

    Grado grado =
        grados
            .findById(request.gradoId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el grado"));
    Seccion seccion =
        secciones
            .findById(request.seccionId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sección"));

    if (!seccion.getGrado().getId().equals(grado.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La sección indicada no pertenece al grado seleccionado");
    }
    validarCruces(seccion, request.fechaInicio(), request.fechaFin());

    Periodo periodo = nuevoPeriodo(seccion, request.fechaInicio(), request.fechaFin());
    return new PeriodoResponse(
        periodo.getId(),
        grado.getId(),
        seccion.getId(),
        periodo.getFechaInicio(),
        periodo.getFechaFin(),
        periodo.calcularEstado(LocalDate.now(clock)));
  }

  /** Crea el periodo de cada sección activa que aún no lo tiene en el año indicado. */
  @Transactional
  public List<VacantesPeriodoResponse> abrirAnhio(AperturaAnhioRequest request) {
    int anhio = request.anhio();
    if (request.fechaInicio().getYear() != anhio) {
      throw new BusinessException(
          "La fecha de inicio de clases debe pertenecer al año escolar " + anhio);
    }
    if (!request.fechaFin().isAfter(request.fechaInicio())) {
      throw new BusinessException(
          "La fecha de fin de clases debe ser posterior a la fecha de inicio");
    }
    if (request.fechaInicio().isBefore(LocalDate.now(clock))) {
      throw new BusinessException("El inicio de clases no puede ser una fecha pasada");
    }

    asegurarEstructuraSecundaria();
    List<Seccion> activas = secciones.findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc();
    if (activas.isEmpty()) {
      throw new BusinessException("No hay secciones registradas para abrir el año escolar");
    }

    LocalDate inicioAnhio = LocalDate.of(anhio, 1, 1);
    LocalDate finAnhio = LocalDate.of(anhio, 12, 31);
    List<Seccion> pendientes =
        activas.stream()
            .filter(
                seccion ->
                    !periodos.existsBySeccionIdAndActiveTrueAndFechaInicioBetween(
                        seccion.getId(), inicioAnhio, finAnhio))
            .toList();
    if (pendientes.isEmpty()) {
      throw new ConflictException(
          "El año escolar " + anhio + " ya está abierto en todas las secciones");
    }
    pendientes.forEach(
        seccion -> validarCruces(seccion, request.fechaInicio(), request.fechaFin()));

    List<VacantesPeriodoResponse> creados = new ArrayList<>();
    for (Seccion seccion : pendientes) {
      Periodo periodo = nuevoPeriodo(seccion, request.fechaInicio(), request.fechaFin());
      creados.add(vacantes.describir(periodo));
    }
    calendario.guardar(anhio, request.fechaInicio(), request.fechaFin(), request.bimestres());
    return creados;
  }

  /** Al abrir el año se crea la estructura base 1.º-5.º de secundaria y la sección A. */
  private void asegurarEstructuraSecundaria() {
    for (int numero = 1; numero <= 5; numero++) {
      final int gradoNumero = numero;
      Grado grado =
          grados
              .findByNumeroAndNivelEducativoAndActiveTrue(gradoNumero, NivelEducativo.SECUNDARIA)
              .orElseGet(
                  () -> {
                    Grado nuevo = new Grado();
                    nuevo.setNumero(gradoNumero);
                    nuevo.setNivelEducativo(NivelEducativo.SECUNDARIA);
                    return grados.save(nuevo);
                  });
      if (!secciones.existsByGradoIdAndCodigoIgnoreCase(grado.getId(), "A")) {
        Seccion seccion = new Seccion();
        seccion.setCodigo("A");
        seccion.setVacantesTotales(30);
        grado.agregarSeccion(seccion);
        secciones.save(seccion);
      }
    }
  }

  @Transactional
  public List<VacantesPeriodoResponse> actualizarAnhio(AperturaAnhioRequest request) {
    int anhio = request.anhio();
    if (request.fechaInicio().getYear() != anhio
        || !request.fechaFin().isAfter(request.fechaInicio())) {
      throw new BusinessException(
          "Las fechas del periodo deben ser válidas y corresponder al año escolar");
    }
    LocalDate desde = LocalDate.of(anhio, 1, 1);
    LocalDate hasta = LocalDate.of(anhio, 12, 31);
    List<Periodo> existentes = periodos.buscar(desde, hasta, null, null);
    if (existentes.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El año escolar no está abierto");
    validarFechasEdicionAnhio(existentes, request);
    List<Integer> ids = existentes.stream().map(Periodo::getId).toList();
    for (Periodo periodo : existentes) {
      List<Periodo> cruces =
          periodos.buscarCruces(
              periodo.getSeccion().getId(), request.fechaInicio(), request.fechaFin());
      if (cruces.stream().anyMatch(cruce -> !ids.contains(cruce.getId()))) {
        throw new ConflictException(
            "Las fechas se cruzan con otro periodo de la sección "
                + periodo.getSeccion().getCodigo());
      }
    }
    existentes.forEach(
        periodo -> {
          periodo.setFechaInicio(request.fechaInicio());
          periodo.setFechaFin(request.fechaFin());
          periodo.setEstado(periodo.calcularEstado(LocalDate.now(clock)));
        });
    List<VacantesPeriodoResponse> actualizados =
        periodos.saveAll(existentes).stream().map(vacantes::describir).toList();
    calendario.guardar(anhio, request.fechaInicio(), request.fechaFin(), request.bimestres());
    return actualizados;
  }

  /**
   * Si las clases del año ya empezaron, el inicio no se mueve y el fin no puede quedar en el
   * pasado; si aún no empiezan, el nuevo inicio no puede ser una fecha pasada.
   */
  private void validarFechasEdicionAnhio(List<Periodo> existentes, AperturaAnhioRequest request) {
    LocalDate hoy = LocalDate.now(clock);
    LocalDate inicioActual =
        existentes.stream().map(Periodo::getFechaInicio).min(LocalDate::compareTo).orElseThrow();
    if (inicioActual.isAfter(hoy)) {
      if (request.fechaInicio().isBefore(hoy)) {
        throw new BusinessException("El inicio de clases no puede ser una fecha pasada");
      }
      return;
    }
    if (!request.fechaInicio().equals(inicioActual)) {
      throw new BusinessException(
          "Las clases ya empezaron el "
              + inicioActual.format(FORMATO_FECHA)
              + "; la fecha de inicio no se puede modificar");
    }
    if (request.fechaFin().isBefore(hoy)) {
      throw new BusinessException("El fin de clases no puede ser una fecha pasada");
    }
  }

  private void validarCruces(Seccion seccion, LocalDate fechaInicio, LocalDate fechaFin) {
    List<Periodo> cruces = periodos.buscarCruces(seccion.getId(), fechaInicio, fechaFin);
    if (!cruces.isEmpty()) {
      Periodo cruce = cruces.get(0);
      throw new ConflictException(
          "Las fechas se cruzan con el periodo de "
              + NombreSeccion.completa(
                  seccion.getGrado().getNumero(),
                  seccion.getCodigo(),
                  seccion.getGrado().getNivelEducativo())
              + ", que va del "
              + cruce.getFechaInicio().format(FORMATO_FECHA)
              + " al "
              + cruce.getFechaFin().format(FORMATO_FECHA));
    }
  }

  private Periodo nuevoPeriodo(Seccion seccion, LocalDate fechaInicio, LocalDate fechaFin) {
    Periodo periodo = new Periodo();
    periodo.setGrado(seccion.getGrado());
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(fechaInicio);
    periodo.setFechaFin(fechaFin);
    periodo.setEstado(periodo.calcularEstado(LocalDate.now(clock)));
    return periodos.save(periodo);
  }
}
