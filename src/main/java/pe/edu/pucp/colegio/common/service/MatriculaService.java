package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.MatriculaCreateRequest;
import pe.edu.pucp.colegio.common.dto.MatriculaResponse;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

@Service
public class MatriculaService {

  private final MatriculaRepository matriculas;
  private final EstudianteRepository estudiantes;
  private final PeriodoRepository periodos;
  private final VacantesService vacantes;

  public MatriculaService(
      MatriculaRepository matriculas,
      EstudianteRepository estudiantes,
      PeriodoRepository periodos,
      VacantesService vacantes) {
    this.matriculas = matriculas;
    this.estudiantes = estudiantes;
    this.periodos = periodos;
    this.vacantes = vacantes;
  }

  @Transactional
  public MatriculaResponse crear(Integer periodoId, MatriculaCreateRequest request) {
    Periodo periodo = buscarPeriodo(periodoId);
    Estudiante estudiante =
        estudiantes
            .findById(request.estudianteId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el estudiante indicado"));

    Matricula matricula =
        matriculas
            .findByPeriodoIdAndEstudianteId(periodoId, estudiante.getId())
            .orElse(null);
    if (matricula != null && Boolean.TRUE.equals(matricula.getActive())
        && matricula.getEstado() != EstadoMatricula.RETIRADA) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El estudiante ya está matriculado en este periodo");
    }

    if (vacantes.consultar(periodoId).vacantesDisponibles() == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo no tiene vacantes");
    }

    if (matricula == null) {
      matricula = new Matricula();
      matricula.setPeriodo(periodo);
      matricula.setEstudiante(estudiante);
    }
    matricula.setFechaMatricula(LocalDate.now());
    matricula.setEstado(EstadoMatricula.EN_REGISTRO);
    matricula.setActive(true);
    matricula = matriculas.save(matricula);

    return respuesta(matricula);
  }

  @Transactional
  public void eliminar(Integer periodoId, Integer estudianteId) {
    buscarPeriodo(periodoId);
    Matricula matricula =
        matriculas
            .findByPeriodoIdAndEstudianteId(periodoId, estudianteId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El estudiante no tiene matrícula en el periodo indicado"));
    matriculas.delete(matricula);
  }

  private Periodo buscarPeriodo(Integer periodoId) {
    return periodos
        .findById(periodoId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el periodo"));
  }

  private MatriculaResponse respuesta(Matricula matricula) {
    return new MatriculaResponse(
        matricula.getId(),
        matricula.getEstudiante().getId(),
        matricula.getPeriodo().getId(),
        matricula.getFechaMatricula(),
        matricula.getEstado());
  }
}
