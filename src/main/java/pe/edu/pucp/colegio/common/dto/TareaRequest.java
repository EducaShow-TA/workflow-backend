package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TareaRequest(
    @NotNull Integer periodoId,
    @NotBlank @Size(max = 50) String competenciaCodigo,
    @NotBlank @Size(max = 180) String titulo,
    @NotBlank @Size(max = 5000) String descripcion,
    @NotNull LocalDateTime fechaLimite,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal ponderacion,
    Integer evaluacionParcialId) {}
