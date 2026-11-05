package pe.edu.pucp.colegio.common.dto;

import pe.edu.pucp.colegio.common.model.EstadoAsistencia;

public record AsistenciaJustificacionResponse(
    Integer asistenciaId,
    Integer estudianteId,
    EstadoAsistencia estado,
    String motivo,
    String documentoNombre,
    String documentoTipo,
    Long documentoTamanio,
    String documentoUrl) {}
