package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRecuperacionPasswordRequestDTO(
    @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50, message = "El usuario es demasiado largo")
        String username) {}
