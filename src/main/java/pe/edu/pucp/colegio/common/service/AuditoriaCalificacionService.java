package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.audit.AccionesCalificacion;
import pe.edu.pucp.colegio.common.audit.AuditoriaCalificacion;
import pe.edu.pucp.colegio.common.audit.AuditoriaCalificacionRepository;
import pe.edu.pucp.colegio.common.audit.GrupoAccionCalificacion;
import pe.edu.pucp.colegio.common.dto.AuditoriaCalificacionResponse;
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.model.CalificacionParcial;
import pe.edu.pucp.colegio.common.repository.CompetenciaRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

/** Registra y consulta cambios académicos auditables en las calificaciones. */
@Service
public class AuditoriaCalificacionService {

  private static final String TIPO_FINAL = "FINAL";
  private static final String TIPO_PARCIAL = "PARCIAL";

  private final AuditoriaCalificacionRepository auditoria;
  private final MatriculaRepository matriculas;
  private final CompetenciaRepository competencias;
  private final Clock clock;

  public AuditoriaCalificacionService(
      AuditoriaCalificacionRepository auditoria,
      MatriculaRepository matriculas,
      CompetenciaRepository competencias,
      Clock clock) {
    this.auditoria = auditoria;
    this.matriculas = matriculas;
    this.competencias = competencias;
    this.clock = clock;
  }

  public FinalAnterior capturarFinal(Calificacion calificacion) {
    return calificacion == null || !Boolean.TRUE.equals(calificacion.getActive())
        ? null
        : new FinalAnterior(
            calificacion.getNota(),
            calificacion.getPuntajeConsolidado(),
            calificacion.getObservacion(),
            calificacion.getConsolidada(),
            calificacion.getBimestre());
  }

  @Transactional
  public void registrarFinal(
      String usuario, String rol, String accion, FinalAnterior anterior, Calificacion nueva) {
    registrarFinal(usuario, rol, accion, anterior, nueva, null);
  }

  @Transactional
  public void registrarFinal(
      String usuario,
      String rol,
      String accion,
      FinalAnterior anterior,
      Calificacion nueva,
      String motivo) {
    String notaAnterior = anterior == null ? null : anterior.nota();
    BigDecimal puntajeAnterior = anterior == null ? null : anterior.puntaje();
    String observacionAnterior = anterior == null ? null : anterior.observacion();
    String notaNueva =
        nueva == null || !Boolean.TRUE.equals(nueva.getActive()) ? null : nueva.getNota();
    BigDecimal puntajeNuevo =
        nueva == null || !Boolean.TRUE.equals(nueva.getActive())
            ? null
            : nueva.getPuntajeConsolidado();
    Boolean consolidadaNueva =
        nueva == null || !Boolean.TRUE.equals(nueva.getActive()) ? null : nueva.getConsolidada();
    String observacionNueva =
        nueva == null || !Boolean.TRUE.equals(nueva.getActive()) ? null : nueva.getObservacion();
    Integer bimestreNuevo =
        nueva == null || !Boolean.TRUE.equals(nueva.getActive()) ? null : nueva.getBimestre();
    if (motivo == null
        && Objects.equals(notaAnterior, notaNueva)
        && Objects.equals(puntajeAnterior, puntajeNuevo)
        && Objects.equals(anterior == null ? null : anterior.consolidada(), consolidadaNueva)
        && Objects.equals(anterior == null ? null : anterior.bimestre(), bimestreNuevo)
        && Objects.equals(observacionAnterior, observacionNueva)) {
      return;
    }

    Calificacion referencia = nueva;
    if (referencia == null) {
      return;
    }
    AuditoriaCalificacion registro =
        new AuditoriaCalificacion(
            LocalDateTime.now(clock),
            usuario,
            rol,
            accion,
            TIPO_FINAL,
            referencia.getPeriodo().getId(),
            referencia.getMatricula().getId(),
            referencia.getMatricula().getEstudiante().getId(),
            referencia.getCompetencia().getCodigo(),
            null,
            referencia.getBimestre(),
            notaAnterior,
            notaNueva,
            puntajeAnterior,
            puntajeNuevo,
            anterior == null ? null : anterior.consolidada(),
            consolidadaNueva,
            null,
            null,
            observacionAnterior,
            observacionNueva);
    registro.setMotivo(motivo);
    auditoria.save(registro);
  }

  public ParcialAnterior capturarParcial(CalificacionParcial calificacion) {
    return calificacion == null || !Boolean.TRUE.equals(calificacion.getActive())
        ? null
        : new ParcialAnterior(calificacion.getNota(), calificacion.getJustificada());
  }

  @Transactional
  public void registrarParcial(
      String usuario,
      String rol,
      String accion,
      ParcialAnterior anterior,
      CalificacionParcial nueva) {
    registrarParcial(usuario, rol, accion, anterior, nueva, null);
  }

  @Transactional
  public void registrarParcial(
      String usuario,
      String rol,
      String accion,
      ParcialAnterior anterior,
      CalificacionParcial nueva,
      String motivo) {
    String notaAnterior = anterior == null ? null : anterior.nota();
    Boolean justificadaAnterior = anterior == null ? null : anterior.justificada();
    String notaNueva =
        nueva == null || !Boolean.TRUE.equals(nueva.getActive()) ? null : nueva.getNota();
    Boolean justificadaNueva =
        nueva == null || !Boolean.TRUE.equals(nueva.getActive()) ? null : nueva.getJustificada();
    if (Objects.equals(notaAnterior, notaNueva)
        && Objects.equals(justificadaAnterior, justificadaNueva)) {
      return;
    }

    CalificacionParcial referencia = nueva;
    if (referencia == null) {
      return;
    }
    AuditoriaCalificacion registro =
        new AuditoriaCalificacion(
            LocalDateTime.now(clock),
            usuario,
            rol,
            accion,
            TIPO_PARCIAL,
            referencia.getEvaluacion().getPeriodo().getId(),
            referencia.getMatricula().getId(),
            referencia.getMatricula().getEstudiante().getId(),
            referencia.getEvaluacion().getCompetencia().getCodigo(),
            referencia.getEvaluacion().getId(),
            referencia.getEvaluacion().getBimestre(),
            notaAnterior,
            notaNueva,
            null,
            null,
            null,
            null,
            justificadaAnterior,
            justificadaNueva,
            null,
            null);
    registro.setMotivo(motivo);
    auditoria.save(registro);
  }

  @Transactional(readOnly = true)
  public Page<AuditoriaCalificacionResponse> listar(
      Integer periodoId,
      Integer matriculaId,
      Integer estudianteId,
      String competenciaCodigo,
      String tipoCalificacion,
      GrupoAccionCalificacion grupo,
      Pageable pageable) {
    var registros =
        auditoria.buscar(
            periodoId,
            matriculaId,
            estudianteId,
            competenciaCodigo,
            tipoCalificacion,
            grupo == null,
            grupo == null ? List.of("") : grupo.acciones(),
            pageable);
    var estudiantesPorMatricula =
        matriculas
            .findAllByIdIn(
                registros.getContent().stream()
                    .map(AuditoriaCalificacion::getMatriculaId)
                    .distinct()
                    .toList())
            .stream()
            .collect(
                Collectors.toMap(
                    m -> m.getId(), m -> m.getEstudiante().getPersona().getNombreCompleto()));
    var competenciasPorCodigo =
        competencias
            .findAllByCodigoIn(
                registros.getContent().stream()
                    .map(AuditoriaCalificacion::getCompetenciaCodigo)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(competencia -> competencia.getCodigo(), Function.identity()));
    return registros.map(
        registro -> {
          var competencia = competenciasPorCodigo.get(registro.getCompetenciaCodigo());
          String area =
              competencia == null || competencia.getAreas().isEmpty()
                  ? null
                  : competencia.getAreas().iterator().next().getNombre();
          return AuditoriaCalificacionResponse.desde(
              registro,
              estudiantesPorMatricula.get(registro.getMatriculaId()),
              competencia == null ? null : competencia.getNombre(),
              area);
        });
  }

  @Transactional(readOnly = true)
  public List<AuditoriaCalificacion> historialFinal(Calificacion calificacion) {
    return auditoria.historialFinal(
        calificacion.getMatricula().getId(),
        calificacion.getCompetencia().getCodigo(),
        calificacion.getBimestre(),
        List.of(AccionesCalificacion.REGISTRAR_O_ACTUALIZAR, AccionesCalificacion.RECTIFICAR));
  }

  @Transactional(readOnly = true)
  public Map<String, Long> rectificacionesFinales(
      Integer periodoId, String competenciaCodigo, Integer bimestre) {
    return auditoria
        .contarPorMatricula(AccionesCalificacion.RECTIFICAR, periodoId, competenciaCodigo, bimestre)
        .stream()
        .collect(
            Collectors.toMap(
                fila -> claveNota((Integer) fila[0], (Integer) fila[1]),
                fila -> (Long) fila[2],
                Long::sum));
  }

  public static String claveNota(Integer matriculaId, Integer bimestre) {
    return matriculaId + "-" + bimestre;
  }

  public record FinalAnterior(
      String nota, BigDecimal puntaje, String observacion, Boolean consolidada, Integer bimestre) {}

  public record ParcialAnterior(String nota, Boolean justificada) {}
}
