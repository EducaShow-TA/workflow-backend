package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DiaNoLectivoRequest(
    @NotNull LocalDate fecha, @NotBlank @Size(max = 120) String descripcion) {}
