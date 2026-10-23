package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaResponse;
import pe.edu.pucp.colegio.common.model.EstadoPeriodoMatricula;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;
import pe.edu.pucp.colegio.common.repository.PeriodoMatriculaRepository;

/** Gestiona las ventanas de tiempo habilitadas para registrar matrículas. */
@Service
public class PeriodoMatriculaService {

  private final PeriodoMatriculaRepository periodos;

  public PeriodoMatriculaService(PeriodoMatriculaRepository periodos) {
    this.periodos = periodos;
  }

  @Transactional
  public PeriodoMatriculaResponse crear(PeriodoMatriculaRequest request) {
    validar(request);
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
    validar(request);
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

  private void validar(PeriodoMatriculaRequest request) {
    if (!request.fechaLimite().isAfter(request.fechaInicio())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha límite debe ser posterior a la fecha de inicio");
    }
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

  private PeriodoMatriculaResponse respuesta(PeriodoMatricula periodo) {
    LocalDate hoy = LocalDate.now();
    EstadoPeriodoMatricula estado;
    if (hoy.isBefore(periodo.getFechaInicio())) {
      estado = EstadoPeriodoMatricula.PENDIENTE;
    } else if (hoy.isAfter(periodo.getFechaLimite())) {
      estado = EstadoPeriodoMatricula.VENCIDO;
    } else {
      estado = EstadoPeriodoMatricula.VIGENTE;
    }
    return new PeriodoMatriculaResponse(
        periodo.getId(), periodo.getFechaInicio(), periodo.getFechaLimite(), estado);
  }
}
