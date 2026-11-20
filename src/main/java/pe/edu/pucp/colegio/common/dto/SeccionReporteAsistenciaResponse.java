package pe.edu.pucp.colegio.common.dto;

/** Sección que el usuario puede usar como filtro del reporte de asistencia. */
public record SeccionReporteAsistenciaResponse(
    Integer seccionId, String seccion, Integer gradoId, String grado) {}
