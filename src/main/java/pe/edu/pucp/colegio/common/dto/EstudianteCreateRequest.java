package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EstudianteCreateRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Size(max = 15) String dni,
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 100) String apellidoPaterno,
    @NotBlank @Size(max = 100) String apellidoMaterno,
    LocalDate fechaNacimiento,
    @Size(max = 20) String telefono,
    @Size(max = 20) String genero) {}
