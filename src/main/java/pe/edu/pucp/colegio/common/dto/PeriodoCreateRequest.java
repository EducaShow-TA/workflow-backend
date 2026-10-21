package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PeriodoCreateRequest(
    @NotNull Integer gradoId,
    @NotNull Integer seccionId,
    @NotNull LocalDate fechaInicio,
    @NotNull LocalDate fechaFin) {}
