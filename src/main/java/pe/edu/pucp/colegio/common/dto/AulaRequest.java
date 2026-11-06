package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AulaRequest(
        @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9-]+") String codigo,
        @NotNull @Min(1) @Max(200) Integer capacidad,
        @Size(max = 200) String descripcion) {}
