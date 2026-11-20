package pe.edu.pucp.colegio.common.dto;

import java.time.DayOfWeek;

/** Bloque semanal asignado al docente autenticado. */
public record HorarioDocenteResponse(
    Integer id,
    Integer horarioId,
    Integer seccionId,
    Integer bloqueId,
    Integer aulaId,
    DayOfWeek dia,
    String horaInicio,
    String horaFin,
    String areaCodigo,
    String areaNombre,
    Integer gradoNumero,
    String seccionCodigo,
    String aulaCodigo) {}
