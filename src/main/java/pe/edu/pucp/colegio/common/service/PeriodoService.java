package pe.edu.pucp.colegio.common.service;

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

  public PeriodoService(
      PeriodoRepository periodos,
      GradoRepository grados,
      SeccionRepository secciones,
      VacantesService vacantes) {
    this.periodos = periodos;
    this.grados = grados;
    this.secciones = secciones;
    this.vacantes = vacantes;
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
        periodo.calcularEstado(LocalDate.now()));
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
    return creados;
  }

  @Transactional
  public List<VacantesPeriodoResponse> actualizarAnhio(AperturaAnhioRequest request) {
    int anhio = request.anhio();
    if (request.fechaInicio().getYear() != anhio || !request.fechaFin().isAfter(request.fechaInicio())) {
      throw new BusinessException("Las fechas del periodo deben ser válidas y corresponder al año escolar");
    }
    LocalDate desde = LocalDate.of(anhio, 1, 1);
    LocalDate hasta = LocalDate.of(anhio, 12, 31);
    List<Periodo> existentes = periodos.buscar(desde, hasta, null, null);
    if (existentes.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El año escolar no está abierto");
    List<Integer> ids = existentes.stream().map(Periodo::getId).toList();
    for (Periodo periodo : existentes) {
      List<Periodo> cruces = periodos.buscarCruces(periodo.getSeccion().getId(), request.fechaInicio(), request.fechaFin());
      if (cruces.stream().anyMatch(cruce -> !ids.contains(cruce.getId()))) {
        throw new ConflictException("Las fechas se cruzan con otro periodo de la sección " + periodo.getSeccion().getCodigo());
      }
    }
    existentes.forEach(periodo -> {
      periodo.setFechaInicio(request.fechaInicio());
      periodo.setFechaFin(request.fechaFin());
      periodo.setEstado(periodo.calcularEstado(LocalDate.now()));
    });
    return periodos.saveAll(existentes).stream().map(vacantes::describir).toList();
  }

  private void validarCruces(Seccion seccion, LocalDate fechaInicio, LocalDate fechaFin) {
    List<Periodo> cruces = periodos.buscarCruces(seccion.getId(), fechaInicio, fechaFin);
    if (!cruces.isEmpty()) {
      Periodo cruce = cruces.get(0);
      throw new ConflictException(
          "Las fechas se cruzan con el periodo de la sección "
              + seccion.getCodigo()
              + " de "
              + seccion.getGrado().getNumero()
              + "° grado, que va del "
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
    periodo.setEstado(periodo.calcularEstado(LocalDate.now()));
    return periodos.save(periodo);
  }
}
