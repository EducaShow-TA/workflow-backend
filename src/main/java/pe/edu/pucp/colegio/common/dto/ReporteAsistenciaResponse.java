package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.util.List;

/** Reporte de inasistencias y tardanzas por alumno en un rango de fechas (US-2.6). */
public record ReporteAsistenciaResponse(
    LocalDate desde,
    LocalDate hasta,
    TotalesAsistenciaResponse totales,
    List<FilaReporteAsistenciaResponse> alumnos) {}
