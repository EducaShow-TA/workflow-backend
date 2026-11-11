package pe.edu.pucp.colegio.common.dto;

import java.util.List;

/** Error de validación masiva con el detalle de cada estudiante afectado. */
public record ErrorConfirmacionMatriculaResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path,
    List<IncidenciaConfirmacionMatriculaResponse> incidencias) {}
