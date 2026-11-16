package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EvaluacionParcialActualizacionRequest(
    @NotBlank @Size(max = 180) String titulo,
    @Size(max = 1000) String descripcion,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("100.0")
        BigDecimal ponderacion,
    @NotNull @Min(1) @Max(4) Integer bimestre) {}
