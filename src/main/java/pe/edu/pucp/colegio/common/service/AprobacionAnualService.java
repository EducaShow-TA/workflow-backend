package pe.edu.pucp.colegio.common.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.SituacionMatriculaResponse;
import pe.edu.pucp.colegio.common.dto.SituacionMatriculaResponse.TipoSituacion;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.CalificacionRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

@Service
public class AprobacionAnualService {

  private static final int ULTIMO_GRADO_SECUNDARIA = 5;

  private final MatriculaRepository matriculas;
  private final CalificacionRepository calificaciones;
  private final EstudianteRepository estudiantes;

  public AprobacionAnualService(
      MatriculaRepository matriculas,
      CalificacionRepository calificaciones,
      EstudianteRepository estudiantes) {
    this.matriculas = matriculas;
    this.calificaciones = calificaciones;
    this.estudiantes = estudiantes;
  }

  /** Situación de cada estudiante activo para matricularse en el año indicado. */
  @Transactional(readOnly = true)
  public List<SituacionMatriculaResponse> situaciones(int anhioDestino) {
    return estudiantes.findAllByActiveTrueOrderByIdAsc().stream()
        .map(estudiante -> situacion(estudiante.getId(), estudiante.getMatriculas(), anhioDestino))
        .toList();
  }

  /**
   * Quien aprobó pasa al grado siguiente (o egresa si terminó 5.º de secundaria); quien no aprobó
   * repite el mismo grado. Un estudiante sin matrículas anteriores es nuevo y entra a cualquier
   * grado.
   */
  @Transactional(readOnly = true)
  public void validarDestino(Integer estudianteId, Periodo destino) {
    int anhioDestino = destino.getFechaInicio().getYear();
    List<Matricula> historial = matriculas.findAllByEstudianteIdIn(List.of(estudianteId));
    SituacionMatriculaResponse situacion = situacion(estudianteId, historial, anhioDestino);
    String nombre = nombreEstudiante(historial);
    int gradoDestino = destino.getGrado().getNumero();
    switch (situacion.situacion()) {
      case NUEVO -> {}
      case PROMOVIDO -> {
        if (gradoDestino != situacion.gradoDestino()) {
          throw conflicto(
              nombre
                  + " aprobó "
                  + situacion.gradoAnterior()
                  + ".º en "
                  + situacion.anhioAnterior()
                  + ": debe matricularse en "
                  + situacion.gradoDestino()
                  + ".º");
        }
      }
      case REPITE -> {
        if (gradoDestino != situacion.gradoDestino()) {
          throw conflicto(
              nombre
                  + " no aprobó "
                  + situacion.gradoAnterior()
                  + ".º en "
                  + situacion.anhioAnterior()
                  + ": debe matricularse otra vez en "
                  + situacion.gradoDestino()
                  + ".º");
        }
      }
      case EGRESADO ->
          throw conflicto(
              nombre
                  + " aprobó "
                  + situacion.gradoAnterior()
                  + ".º en "
                  + situacion.anhioAnterior()
                  + ": ya egresó del colegio");
      case BLOQUEADO -> throw conflicto(nombre + ": " + situacion.motivo());
    }
  }

  @Transactional(readOnly = true)
  public boolean tieneMatriculaAnterior(Integer estudianteId, int anhioDestino) {
    return matriculas.findAllByEstudianteIdIn(List.of(estudianteId)).stream()
        .anyMatch(
            matricula ->
                esMatriculaFormal(matricula)
                    && matricula.getPeriodo().getFechaInicio().getYear() == anhioDestino - 1);
  }

  @Transactional(readOnly = true)
  public boolean tuvoMatriculaAnterior(Integer estudianteId, int anhioDestino) {
    return matriculas.findAllByEstudianteIdIn(List.of(estudianteId)).stream()
        .anyMatch(
            matricula ->
                esMatriculaFormal(matricula)
                    && matricula.getPeriodo().getFechaInicio().getYear() < anhioDestino);
  }

  private SituacionMatriculaResponse situacion(
      Integer estudianteId, Collection<Matricula> historial, int anhioDestino) {
    Matricula anterior =
        historial.stream()
            .filter(this::esMatriculaFormal)
            .filter(m -> m.getPeriodo().getFechaInicio().getYear() < anhioDestino)
            .max(Comparator.comparing(m -> m.getPeriodo().getFechaInicio()))
            .orElse(null);
    if (anterior == null) {
      return new SituacionMatriculaResponse(
          estudianteId, TipoSituacion.NUEVO, null, null, null, null);
    }
    Periodo periodoAnterior = anterior.getPeriodo();
    int anhioAnterior = periodoAnterior.getFechaInicio().getYear();
    int gradoAnterior = periodoAnterior.getGrado().getNumero();
    if (anhioAnterior != anhioDestino - 1) {
      return bloqueado(
          estudianteId,
          gradoAnterior,
          anhioAnterior,
          "falta su matrícula confirmada del año escolar " + (anhioDestino - 1));
    }
    if (!Boolean.TRUE.equals(periodoAnterior.getActasCerradas())) {
      return bloqueado(
          estudianteId,
          gradoAnterior,
          anhioAnterior,
          "faltan cerrar las actas de " + anhioAnterior);
    }
    Boolean aprobo = aprobo(anterior);
    if (aprobo == null) {
      return bloqueado(
          estudianteId,
          gradoAnterior,
          anhioAnterior,
          "no tiene notas anuales consolidadas de " + anhioAnterior);
    }
    if (!aprobo) {
      return new SituacionMatriculaResponse(
          estudianteId, TipoSituacion.REPITE, gradoAnterior, gradoAnterior, anhioAnterior, null);
    }
    if (esUltimoGrado(periodoAnterior.getGrado())) {
      return new SituacionMatriculaResponse(
          estudianteId, TipoSituacion.EGRESADO, gradoAnterior, null, anhioAnterior, null);
    }
    return new SituacionMatriculaResponse(
        estudianteId,
        TipoSituacion.PROMOVIDO,
        gradoAnterior,
        gradoAnterior + 1,
        anhioAnterior,
        null);
  }

  private SituacionMatriculaResponse bloqueado(
      Integer estudianteId, int gradoAnterior, int anhioAnterior, String motivo) {
    return new SituacionMatriculaResponse(
        estudianteId, TipoSituacion.BLOQUEADO, gradoAnterior, null, anhioAnterior, motivo);
  }

  private boolean esUltimoGrado(Grado grado) {
    NivelEducativo nivel = grado.getNivelEducativo();
    return (nivel == null || nivel == NivelEducativo.SECUNDARIA)
        && grado.getNumero() >= ULTIMO_GRADO_SECUNDARIA;
  }

  private boolean esMatriculaFormal(Matricula matricula) {
    return Boolean.TRUE.equals(matricula.getActive())
        && (matricula.getEstado() == EstadoMatricula.CONFIRMADA
            || matricula.getEstado() == EstadoMatricula.FINALIZADA);
  }

  /** Devuelve null cuando no hay notas anuales consolidadas para decidir. */
  private Boolean aprobo(Matricula matricula) {
    var notas =
        calificaciones
            .findAllByPeriodoIdAndMatriculaIdAndConsolidadaTrueAndBimestreIsNullAndActiveTrue(
                matricula.getPeriodo().getId(), matricula.getId());
    if (notas.isEmpty()) {
      return null;
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

  private String nombreEstudiante(List<Matricula> historial) {
    return historial.stream()
        .map(Matricula::getEstudiante)
        .filter(estudiante -> estudiante != null && estudiante.getPersona() != null)
        .map(Estudiante::getPersona)
        .map(this::nombreCompleto)
        .findFirst()
        .orElse("El estudiante");
  }

  private String nombreCompleto(Persona persona) {
    return String.join(
            " ",
            persona.getNombre() == null ? "" : persona.getNombre(),
            persona.getApellidoPaterno() == null ? "" : persona.getApellidoPaterno(),
            persona.getApellidoMaterno() == null ? "" : persona.getApellidoMaterno())
        .trim();
  }

  private ResponseStatusException conflicto(String mensaje) {
    return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
  }
}
