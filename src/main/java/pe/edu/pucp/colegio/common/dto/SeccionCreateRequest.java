package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SeccionCreateRequest(
    @NotNull @Min(1) @Max(5) Integer gradoNumero,
    @NotBlank @Pattern(regexp = "[A-Z]{1,2}") String nombre,
    @NotNull @Positive Integer vacantesTotales) {}
