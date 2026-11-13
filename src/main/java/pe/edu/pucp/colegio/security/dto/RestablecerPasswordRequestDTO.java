package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.NotBlank;
import pe.edu.pucp.colegio.common.validation.PasswordPolicy;

public record RestablecerPasswordRequestDTO(
    @NotBlank(message = "El token es obligatorio") String token,
    @NotBlank(message = "La nueva contraseña es obligatoria") @PasswordPolicy String password) {}
