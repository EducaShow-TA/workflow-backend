package pe.edu.pucp.colegio.common.dto;

import java.time.DayOfWeek;

/** Bloque semanal de una sección donde el alumno tiene matrícula confirmada. */
public record HorarioEstudianteResponse(
    Integer id,
    DayOfWeek dia,
    String horaInicio,
    String horaFin,
    String areaCodigo,
    String areaNombre,
    String docenteNombre,
    Integer gradoNumero,
    String seccionCodigo,
    String aulaCodigo) {}
