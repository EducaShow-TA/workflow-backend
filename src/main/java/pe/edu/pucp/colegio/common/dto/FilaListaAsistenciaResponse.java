package pe.edu.pucp.colegio.common.dto;

import pe.edu.pucp.colegio.common.model.EstadoAsistencia;

/** Alumno de la sesión con su asistencia; {@code estado} es nulo si aún no se registró. */
public record FilaListaAsistenciaResponse(
    Integer estudianteId,
    String dni,
    String nombreCompleto,
    Integer asistenciaId,
    EstadoAsistencia estado,
    boolean matriculado) {}
