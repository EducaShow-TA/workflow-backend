package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import pe.edu.pucp.colegio.common.model.MedioPago;

public record RegistrarPagoRequest(
    @NotNull @DecimalMin("0.01") BigDecimal monto,
    @NotNull MedioPago medioPago,
    @Size(max = 40) String referencia) {}
