package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.EstadoPeriodoMatricula;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoMatriculaRepository;

/** Gestiona las ventanas de tiempo habilitadas para registrar matrículas. */
@Service
public class PeriodoMatriculaService {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final PeriodoMatriculaRepository periodos;
  private final MatriculaRepository matriculas;

  public PeriodoMatriculaService(
      PeriodoMatriculaRepository periodos, MatriculaRepository matriculas) {
    this.periodos = periodos;
    this.matriculas = matriculas;
  }

  @Transactional
  public PeriodoMatriculaResponse crear(PeriodoMatriculaRequest request) {
    validar(request, null);
    PeriodoMatricula periodo = new PeriodoMatricula();
    periodo.setFechaInicio(request.fechaInicio());
    periodo.setFechaLimite(request.fechaLimite());
    return respuesta(periodos.save(periodo));
  }

  @Transactional(readOnly = true)
  public List<PeriodoMatriculaResponse> listar() {
    return periodos.findAllByActiveTrueOrderByFechaInicioDesc().stream()
        .map(this::respuesta)
        .toList();
  }

  @Transactional(readOnly = true)
  public PeriodoMatriculaResponse buscar(Integer id) {
    return respuesta(buscarEntidad(id));
  }

  @Transactional
  public PeriodoMatriculaResponse actualizar(Integer id, PeriodoMatriculaRequest request) {
    PeriodoMatricula periodo = buscarEntidad(id);
    if (calcularEstado(periodo) == EstadoPeriodoMatricula.VENCIDO && tieneMatriculas(periodo)) {
      throw new BusinessException(
          "No se puede modificar un periodo de matrícula vencido con matrículas registradas");
    }
    validar(request, id);
    periodo.setFechaInicio(request.fechaInicio());
    periodo.setFechaLimite(request.fechaLimite());
    return respuesta(periodos.save(periodo));
  }

  @Transactional
  public void desactivar(Integer id) {
    PeriodoMatricula periodo = buscarEntidad(id);
    periodo.setActive(false);
    periodos.save(periodo);
  }

  @Transactional(readOnly = true)
  public boolean hayPeriodoVigente() {
    LocalDate hoy = LocalDate.now();
    return periodos.existsByActiveTrueAndFechaInicioLessThanEqualAndFechaLimiteGreaterThanEqual(
        hoy, hoy);
  }

  private void validar(PeriodoMatriculaRequest request, Integer idEnEdicion) {
    if (request.fechaInicio().isBefore(LocalDate.now())) {
      throw new BusinessException("La fecha de inicio no puede ser anterior a hoy");
    }
    if (!request.fechaLimite().isAfter(request.fechaInicio())) {
      throw new BusinessException("La fecha límite debe ser posterior a la fecha de inicio");
    }
    List<PeriodoMatricula> cruces =
        periodos.buscarCruces(request.fechaInicio(), request.fechaLimite(), idEnEdicion);
    if (!cruces.isEmpty()) {
      PeriodoMatricula cruce = cruces.get(0);
      throw new ConflictException(
          "Las fechas se cruzan con el periodo de matrícula del "
              + cruce.getFechaInicio().format(FORMATO_FECHA)
              + " al "
              + cruce.getFechaLimite().format(FORMATO_FECHA));
    }
  }

  private boolean tieneMatriculas(PeriodoMatricula periodo) {
    return matriculas.existsByActiveTrueAndFechaMatriculaBetween(
        periodo.getFechaInicio(), periodo.getFechaLimite());
  }

  private PeriodoMatricula buscarEntidad(Integer id) {
    return periodos
        .findById(id)
        .filter(p -> Boolean.TRUE.equals(p.getActive()))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe el periodo de matrícula"));
  }

  private EstadoPeriodoMatricula calcularEstado(PeriodoMatricula periodo) {
    LocalDate hoy = LocalDate.now();
    if (hoy.isBefore(periodo.getFechaInicio())) {
      return EstadoPeriodoMatricula.PENDIENTE;
    }
    if (hoy.isAfter(periodo.getFechaLimite())) {
      return EstadoPeriodoMatricula.VENCIDO;
    }
    return EstadoPeriodoMatricula.VIGENTE;
  }

  private PeriodoMatriculaResponse respuesta(PeriodoMatricula periodo) {
    return new PeriodoMatriculaResponse(
        periodo.getId(),
        periodo.getFechaInicio(),
        periodo.getFechaLimite(),
        calcularEstado(periodo),
        tieneMatriculas(periodo));
  }
}
