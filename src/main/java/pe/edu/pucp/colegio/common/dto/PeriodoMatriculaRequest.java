package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PeriodoMatriculaRequest(
    @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaLimite) {}
