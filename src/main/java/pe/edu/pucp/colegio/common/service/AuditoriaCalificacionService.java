package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.audit.AuditoriaCalificacion;
import pe.edu.pucp.colegio.common.audit.AuditoriaCalificacionRepository;
import pe.edu.pucp.colegio.common.dto.AuditoriaCalificacionResponse;
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.model.CalificacionParcial;

@Service
public class AuditoriaCalificacionService {

  private static final String TIPO_FINAL = "FINAL";
  private static final String TIPO_PARCIAL = "PARCIAL";

  private final AuditoriaCalificacionRepository auditoria;

  public AuditoriaCalificacionService(AuditoriaCalificacionRepository auditoria) {
    this.auditoria = auditoria;
  }

  public FinalAnterior capturarFinal(Calificacion calificacion) {
    return calificacion == null || !Boolean.TRUE.equals(calificacion.getActive())
        ? null
        : new FinalAnterior(
            calificacion.getNota(),
            calificacion.getPuntajeConsolidado(),
            calificacion.getObservacion(),
            calificacion.getConsolidada());
  }

  @Transactional
  public void registrarFinal(
      String usuario, String rol, String accion, FinalAnterior anterior, Calificacion nueva) {
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
    if (Objects.equals(notaAnterior, notaNueva)
        && Objects.equals(puntajeAnterior, puntajeNuevo)
        && Objects.equals(anterior == null ? null : anterior.consolidada(), consolidadaNueva)
        && Objects.equals(observacionAnterior, observacionNueva)) {
      return;
    }

    Calificacion referencia = nueva;
    if (referencia == null) {
      return;
    }
    auditoria.save(
        new AuditoriaCalificacion(
            LocalDateTime.now(),
            usuario,
            rol,
            accion,
            TIPO_FINAL,
            referencia.getPeriodo().getId(),
            referencia.getMatricula().getId(),
            referencia.getMatricula().getEstudiante().getId(),
            referencia.getCompetencia().getCodigo(),
            null,
            notaAnterior,
            notaNueva,
            puntajeAnterior,
            puntajeNuevo,
            anterior == null ? null : anterior.consolidada(),
            consolidadaNueva,
            null,
            null,
            observacionAnterior,
            observacionNueva));
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
    auditoria.save(
        new AuditoriaCalificacion(
            LocalDateTime.now(),
            usuario,
            rol,
            accion,
            TIPO_PARCIAL,
            referencia.getEvaluacion().getPeriodo().getId(),
            referencia.getMatricula().getId(),
            referencia.getMatricula().getEstudiante().getId(),
            referencia.getEvaluacion().getCompetencia().getCodigo(),
            referencia.getEvaluacion().getId(),
            notaAnterior,
            notaNueva,
            null,
            null,
            null,
            null,
            justificadaAnterior,
            justificadaNueva,
            null,
            null));
  }

  @Transactional(readOnly = true)
  public Page<AuditoriaCalificacionResponse> listar(
      Integer periodoId,
      Integer matriculaId,
      Integer estudianteId,
      String competenciaCodigo,
      String tipoCalificacion,
      Pageable pageable) {
    return auditoria
        .buscar(periodoId, matriculaId, estudianteId, competenciaCodigo, tipoCalificacion, pageable)
        .map(AuditoriaCalificacionResponse::desde);
  }

  public record FinalAnterior(
      String nota, BigDecimal puntaje, String observacion, Boolean consolidada) {}

  public record ParcialAnterior(String nota, Boolean justificada) {}
}
