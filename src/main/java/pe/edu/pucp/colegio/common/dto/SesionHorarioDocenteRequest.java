package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;

/** Datos editables de una sesión del horario propio del docente autenticado. */
public record SesionHorarioDocenteRequest(
    @NotNull Integer bloqueId,
    @NotNull DayOfWeek dia,
    @NotBlank String areaCodigo,
    @NotNull Integer aulaId) {}
