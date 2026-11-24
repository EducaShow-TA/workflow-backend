package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record FechaSistemaRequest(
    @NotNull(message = "Elige la fecha y la hora") LocalDateTime fecha,
    @NotBlank(message = "Escribe el motivo del cambio")
        @Size(min = 10, max = 300, message = "El motivo debe tener entre 10 y 300 caracteres")
        String motivo) {}
