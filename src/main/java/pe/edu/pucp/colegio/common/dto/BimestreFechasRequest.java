package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BimestreFechasRequest(
    @NotNull @Min(1) @Max(4) Integer numero,
    @NotNull(message = "Indica el inicio de cada bimestre") LocalDate fechaInicio,
    @NotNull(message = "Indica el fin de cada bimestre") LocalDate fechaFin) {}
