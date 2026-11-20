package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.edu.pucp.colegio.common.audit.AuditoriaCalificacion;

public record AuditoriaCalificacionResponse(
    Integer id,
    LocalDateTime fechaHora,
    String usuario,
    String rolUsuario,
    String accion,
    String tipoCalificacion,
    Integer periodoId,
    Integer matriculaId,
    Integer estudianteId,
    String estudianteNombre,
    String competenciaNombre,
    String areaNombre,
    String competenciaCodigo,
    Integer evaluacionId,
    Integer bimestre,
    String notaAnterior,
    String notaNueva,
    BigDecimal puntajeAnterior,
    BigDecimal puntajeNuevo,
    Boolean consolidadaAnterior,
    Boolean consolidadaNueva,
    Boolean justificadaAnterior,
    Boolean justificadaNueva,
    String observacionAnterior,
    String observacionNueva,
    String motivo) {

  public static AuditoriaCalificacionResponse desde(AuditoriaCalificacion registro) {
    return desde(registro, null, null, null);
  }

  public static AuditoriaCalificacionResponse desde(
      AuditoriaCalificacion registro,
      String estudianteNombre,
      String competenciaNombre,
      String areaNombre) {
    return new AuditoriaCalificacionResponse(
        registro.getId(),
        registro.getFechaHora(),
        registro.getUsuario(),
        registro.getRolUsuario(),
        registro.getAccion(),
        registro.getTipoCalificacion(),
        registro.getPeriodoId(),
        registro.getMatriculaId(),
        registro.getEstudianteId(),
        estudianteNombre,
        competenciaNombre,
        areaNombre,
        registro.getCompetenciaCodigo(),
        registro.getEvaluacionId(),
        registro.getBimestre(),
        registro.getNotaAnterior(),
        registro.getNotaNueva(),
        registro.getPuntajeAnterior(),
        registro.getPuntajeNuevo(),
        registro.getConsolidadaAnterior(),
        registro.getConsolidadaNueva(),
        registro.getJustificadaAnterior(),
        registro.getJustificadaNueva(),
        registro.getObservacionAnterior(),
        registro.getObservacionNueva(),
        registro.getMotivo());
  }
}
