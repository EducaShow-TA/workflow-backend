package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CalificacionRequest(
    @NotNull Integer periodoId,
    @NotNull Integer matriculaId,
    @NotBlank @Size(max = 50) String competenciaCodigo,
    @Min(1) @Max(4) Integer bimestre,
    @NotBlank @Pattern(regexp = "AD|A|B|C", message = "La nota debe ser AD, A, B o C") String nota,
    @Size(max = 1000) String observacion,
    @Size(max = 2048) String evidenciaUrl,
    @Size(max = 500) String motivo) {}
