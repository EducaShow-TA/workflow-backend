package pe.edu.pucp.colegio.common.dto;

import java.time.DayOfWeek;

/** Bloque semanal asignado al docente autenticado. */
public record HorarioDocenteResponse(
    Integer id,
    DayOfWeek dia,
    String horaInicio,
    String horaFin,
    String areaCodigo,
    String areaNombre,
    Integer gradoNumero,
    String seccionCodigo,
    String aulaCodigo) {}
