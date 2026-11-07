package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecreoHorarioRequest(
    @NotNull @Min(1) @Max(20) Integer despuesDeBloque,
    @NotNull @Positive @Max(240) Integer duracionMinutos) {}
