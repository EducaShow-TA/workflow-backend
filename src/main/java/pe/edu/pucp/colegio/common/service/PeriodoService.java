package pe.edu.pucp.colegio.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.PeriodoCreateRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoResponse;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;

@Service
public class PeriodoService {

  private final PeriodoRepository periodos;
  private final GradoRepository grados;
  private final SeccionRepository secciones;

  public PeriodoService(
      PeriodoRepository periodos, GradoRepository grados, SeccionRepository secciones) {
    this.periodos = periodos;
    this.grados = grados;
    this.secciones = secciones;
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
    if (periodos.existsByGradoIdAndSeccionIdAndFechaInicioAndFechaFin(
        grado.getId(), seccion.getId(), request.fechaInicio(), request.fechaFin())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe un periodo con ese grado, sección y fechas");
    }

    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(request.fechaInicio());
    periodo.setFechaFin(request.fechaFin());
    periodo.setEstado(EstadoPeriodo.PLANIFICADO);
    periodo = periodos.save(periodo);

    return new PeriodoResponse(
        periodo.getId(),
        grado.getId(),
        seccion.getId(),
        periodo.getFechaInicio(),
        periodo.getFechaFin(),
        periodo.getEstado());
  }
}
