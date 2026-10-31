package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.pucp.colegio.common.validation.UsernamePolicy;

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
    @NotBlank(message = "El usuario es obligatorio")
        @Pattern(
            regexp = UsernamePolicy.PATRON,
            message = "El usuario solo puede contener letras y números")
        @Size(max = 50, message = "El usuario es muy largo")
        String username,
    @NotBlank(message = "El correo es obligatorio")
        @jakarta.validation.constraints.Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo es muy largo")
        String correo) {}
