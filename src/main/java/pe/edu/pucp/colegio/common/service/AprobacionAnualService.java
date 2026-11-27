package pe.edu.pucp.colegio.common.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.CalificacionRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

@Service
public class AprobacionAnualService {

  private final MatriculaRepository matriculas;
  private final CalificacionRepository calificaciones;

  public AprobacionAnualService(
      MatriculaRepository matriculas, CalificacionRepository calificaciones) {
    this.matriculas = matriculas;
    this.calificaciones = calificaciones;
  }

  @Transactional(readOnly = true)
  public void validarDestino(Integer estudianteId, Periodo destino) {
    int anhioDestino = destino.getFechaInicio().getYear();
    Matricula anterior =
        matriculas.findAllByEstudianteIdIn(List.of(estudianteId)).stream()
            .filter(m -> Boolean.TRUE.equals(m.getActive()))
            .filter(m -> esMatriculaFormal(m.getEstado()))
            .filter(m -> m.getPeriodo().getFechaInicio().getYear() < anhioDestino)
            .max(
                (a, b) ->
                    a.getPeriodo().getFechaInicio().compareTo(b.getPeriodo().getFechaInicio()))
            .orElse(null);
    if (anterior == null) {
      return;
    }

    int anhioAnterior = anterior.getPeriodo().getFechaInicio().getYear();
    if (anhioAnterior != anhioDestino - 1) {
      throw conflicto(
          "No se puede validar la promoción: falta la matrícula confirmada del año escolar "
              + (anhioDestino - 1));
    }
    int gradoAnterior = anterior.getPeriodo().getGrado().getNumero();
    int gradoDestino = destino.getGrado().getNumero();
    if (gradoDestino > gradoAnterior + 1) {
      throw conflicto("No se puede avanzar más de un grado en un año escolar");
    }
    if (!aprobo(anterior)) {
      throw conflicto(
          "El estudiante debe aprobar el año escolar anterior antes de matricularse en el siguiente");
    }
  }

  @Transactional(readOnly = true)
  public boolean tieneMatriculaAnterior(Integer estudianteId, int anhioDestino) {
    return matriculas.findAllByEstudianteIdIn(List.of(estudianteId)).stream()
        .anyMatch(
            matricula ->
                Boolean.TRUE.equals(matricula.getActive())
                    && esMatriculaFormal(matricula.getEstado())
                    && matricula.getPeriodo().getFechaInicio().getYear() == anhioDestino - 1);
  }

  @Transactional(readOnly = true)
  public boolean tuvoMatriculaAnterior(Integer estudianteId, int anhioDestino) {
    return matriculas.findAllByEstudianteIdIn(List.of(estudianteId)).stream()
        .anyMatch(
            matricula ->
                Boolean.TRUE.equals(matricula.getActive())
                    && esMatriculaFormal(matricula.getEstado())
                    && matricula.getPeriodo().getFechaInicio().getYear() < anhioDestino);
  }

  private boolean esMatriculaFormal(EstadoMatricula estado) {
    return estado == EstadoMatricula.CONFIRMADA || estado == EstadoMatricula.FINALIZADA;
  }

  private boolean aprobo(Matricula matricula) {
    Periodo periodoAnterior = matricula.getPeriodo();
    if (!Boolean.TRUE.equals(periodoAnterior.getActasCerradas())) {
      throw conflicto("Las actas del año anterior deben estar cerradas antes de matricular");
    }
    var notas =
        calificaciones
            .findAllByPeriodoIdAndMatriculaIdAndConsolidadaTrueAndBimestreIsNullAndActiveTrue(
                periodoAnterior.getId(), matricula.getId());
    if (notas.isEmpty()) {
      throw conflicto("No hay calificaciones anuales consolidadas para validar la promoción");
    }

    Map<String, int[]> conteoPorArea = new HashMap<>();
    for (var nota : notas) {
      for (var area : nota.getCompetencia().getAreas()) {
        int[] conteo = conteoPorArea.computeIfAbsent(area.getCodigo(), ignored -> new int[2]);
        conteo[0]++;
        if ("C".equalsIgnoreCase(nota.getNota())) {
          conteo[1]++;
        }
      }
    }
    long areasDesaprobadas =
        conteoPorArea.values().stream().filter(conteo -> conteo[1] * 2 >= conteo[0]).count();
    return areasDesaprobadas < 4;
  }

  private ResponseStatusException conflicto(String mensaje) {
    return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
  }
}
