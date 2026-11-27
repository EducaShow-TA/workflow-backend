package pe.edu.pucp.colegio.common.dto;

/** Situación de un estudiante para matricularse en un año escolar. */
public record SituacionMatriculaResponse(
    Integer estudianteId,
    TipoSituacion situacion,
    Integer gradoAnterior,
    Integer gradoDestino,
    Integer anhioAnterior,
    String motivo) {

  public enum TipoSituacion {
    NUEVO,
    PROMOVIDO,
    REPITE,
    EGRESADO,
    BLOQUEADO
  }
}
