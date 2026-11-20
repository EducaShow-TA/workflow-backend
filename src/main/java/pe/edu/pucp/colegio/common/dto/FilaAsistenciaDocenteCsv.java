package pe.edu.pucp.colegio.common.dto;

/** Fila de datos del CSV de asistencia docente (US-2.5); {@code fila} cuenta desde 1. */
public record FilaAsistenciaDocenteCsv(
    int fila,
    String dniDocente,
    String fecha,
    String horaEntrada,
    String horaSalida,
    String estado,
    String motivo) {}
