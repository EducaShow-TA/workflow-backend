package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRecuperacionPasswordRequestDTO(
    @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 50, message = "El correo es demasiado largo")
        String username) {}
