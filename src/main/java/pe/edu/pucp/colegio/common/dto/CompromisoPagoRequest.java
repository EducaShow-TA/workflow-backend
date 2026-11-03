package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CompromisoPagoRequest(
    @NotBlank String concepto,
    @NotNull @DecimalMin("0.01") BigDecimal montoTotal,
    @NotNull @Min(1) @Max(24) Integer numeroCuotas,
    @NotNull LocalDate primerVencimiento,
    @NotNull @Min(1) @Max(28) Integer diaVencimiento) {}
