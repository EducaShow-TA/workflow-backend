package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarUmbralRiesgoRequest(@NotNull @Min(1) @Max(5) Integer umbral) {}
