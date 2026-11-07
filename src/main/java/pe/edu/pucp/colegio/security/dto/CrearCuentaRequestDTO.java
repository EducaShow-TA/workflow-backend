package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.pucp.colegio.common.validation.UsernamePolicy;
import pe.edu.pucp.colegio.security.model.Rol;

public record CrearCuentaRequestDTO(
    @NotBlank(message = "El DNI es obligatorio") @Size(max = 15, message = "El DNI es muy largo")
        String dni,
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotBlank(message = "El apellido paterno es obligatorio") String apellidoPaterno,
    @NotBlank(message = "El apellido materno es obligatorio") String apellidoMaterno,
    @NotBlank(message = "El usuario es obligatorio")
        @Pattern(
            regexp = UsernamePolicy.PATRON,
            message = "El usuario solo puede contener letras y números")
        @Size(max = 50, message = "El usuario es muy largo")
        String username,
    @NotBlank(message = "El correo es obligatorio")
        @jakarta.validation.constraints.Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo es muy largo")
        String correo,
    @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,
    @NotNull(message = "El rol es obligatorio") Rol rol) {}
