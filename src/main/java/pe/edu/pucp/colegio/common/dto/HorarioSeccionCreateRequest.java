package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HorarioSeccionCreateRequest(
    @NotNull Integer seccionId,
    @NotNull @Min(2000) @Max(2100) Integer anio,
    @NotNull Integer plantillaId) {}
