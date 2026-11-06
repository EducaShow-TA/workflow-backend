package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReglaConsolidacionRequest(
    @NotNull @DecimalMin(value = "1.0", inclusive = false) @DecimalMax("4.0") BigDecimal umbralB,
    @NotNull @DecimalMin(value = "1.0", inclusive = false) @DecimalMax("4.0") BigDecimal umbralA,
    @NotNull @DecimalMin(value = "1.0", inclusive = false) @DecimalMax("4.0")
        BigDecimal umbralAd) {}
