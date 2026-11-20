package pe.edu.pucp.colegio.common.dto;

/** Fila de datos del CSV de asistencia de alumnos de una sesión (US-2.2); {@code fila} desde 1. */
public record FilaAsistenciaEstudianteCsv(int fila, String dniAlumno, String estado) {}
