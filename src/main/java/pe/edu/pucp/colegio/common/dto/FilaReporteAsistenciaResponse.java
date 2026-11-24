package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;

/** Consolidado de un alumno en una sección; el porcentaje solo cuenta faltas sin justificar. */
public record FilaReporteAsistenciaResponse(
    Integer estudianteId,
    String nombreCompleto,
    Integer gradoId,
    Integer gradoNumero,
    String grado,
    Integer seccionId,
    String seccion,
    long presentes,
    long tardanzas,
    long inasistencias,
    long justificadas,
    long totalSesiones,
    BigDecimal porcentajeInasistencia) {}
