package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record SesionClaseRequest(
    @NotNull Integer seccionId,
    @NotBlank @Size(max = 50) String areaCodigo,
    @NotNull Integer docenteId,
    @NotNull LocalDate fecha,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin) {}
