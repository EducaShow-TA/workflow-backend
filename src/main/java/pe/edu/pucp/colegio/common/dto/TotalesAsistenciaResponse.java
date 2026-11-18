package pe.edu.pucp.colegio.common.dto;

/** Suma de los conteos de todas las filas del reporte de asistencia (US-2.6). */
public record TotalesAsistenciaResponse(
    long presentes, long tardanzas, long inasistencias, long justificadas, long totalSesiones) {}
