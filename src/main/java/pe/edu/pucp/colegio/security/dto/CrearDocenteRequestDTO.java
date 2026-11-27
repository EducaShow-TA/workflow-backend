package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearDocenteRequestDTO(
    @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
    @NotBlank(message = "El apellido paterno es obligatorio") @Size(max = 100)
        String apellidoPaterno,
    @NotBlank(message = "El apellido materno es obligatorio") @Size(max = 100)
        String apellidoMaterno,
    @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 254, message = "El correo es muy largo")
        String correo) {}
