package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarCuentaRequestDTO(
    @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre es muy largo")
        String nombre,
    @NotBlank(message = "El apellido paterno es obligatorio")
        @Size(max = 100, message = "El apellido paterno es muy largo")
        String apellidoPaterno,
    @NotBlank(message = "El apellido materno es obligatorio")
        @Size(max = 100, message = "El apellido materno es muy largo")
        String apellidoMaterno,
    @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 50, message = "El correo es muy largo")
        String username) {}
