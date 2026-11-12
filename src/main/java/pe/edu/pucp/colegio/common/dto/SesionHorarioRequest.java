package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;

public record SesionHorarioRequest(
    @NotNull Integer bloqueId,
    @NotNull DayOfWeek dia,
    @NotBlank String areaCodigo,
    @NotNull Integer docenteId,
    @NotNull Integer aulaId) {}
