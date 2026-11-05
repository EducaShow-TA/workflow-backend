package pe.edu.pucp.colegio.common.dto;

import pe.edu.pucp.colegio.common.model.EstadoAsistencia;

public record AsistenciaEstudianteResponse(
    Integer id,
    Integer estudianteId,
    String estudianteNombre,
    Integer sesionClaseId,
    EstadoAsistencia estado,
    String motivoJustificacion,
    String archivoJustificacion) {}
