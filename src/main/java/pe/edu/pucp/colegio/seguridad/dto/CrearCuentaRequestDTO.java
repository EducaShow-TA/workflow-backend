package pe.edu.pucp.colegio.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.pucp.colegio.seguridad.model.Rol;

public record CrearCuentaRequestDTO(
    @NotBlank(message = "El DNI es obligatorio") @Size(max = 15, message = "El DNI es muy largo")
        String dni,
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotBlank(message = "El apellido paterno es obligatorio") String apellidoPaterno,
    @NotBlank(message = "El apellido materno es obligatorio") String apellidoMaterno,
    @NotBlank(message = "El correo es obligatorio") @Size(max = 50, message = "El correo es muy largo")
        String username,
    @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,
    @NotNull(message = "El rol es obligatorio") Rol rol) {}
