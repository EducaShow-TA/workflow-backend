package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pucp.colegio.common.validation.UsernamePolicy;

public record EstudianteCreateRequest(
    @NotBlank
        @Pattern(
            regexp = UsernamePolicy.PATRON,
            message = "El usuario solo puede contener letras y números")
        @Size(max = 50)
        String username,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank @Pattern(regexp = "DNI|PASAPORTE") String tipoDocumento,
    @NotBlank @Size(max = 15) String numeroDocumento,
    @NotBlank @Size(max = 100) String nombres,
    @NotBlank @Size(max = 100) String apellidoPaterno,
    @NotBlank @Size(max = 100) String apellidoMaterno,
    LocalDate fechaNacimiento,
    @Size(max = 20) String celular,
    @Size(max = 20) String genero,
    @jakarta.validation.constraints.Email @Size(max = 254) String correoElectronico,
    @Size(max = 50) String nacionalidad,
    @Size(max = 255) String direccion,
    @NotEmpty @Valid List<ApoderadoCreateRequest> apoderados) {

  public record ApoderadoCreateRequest(
      @NotBlank @Pattern(regexp = "DNI|PASAPORTE") String tipoDocumento,
      @NotBlank @Size(max = 15) String numeroDocumento,
      @NotBlank @Size(max = 100) String nombres,
      @NotBlank @Size(max = 100) String apellidoPaterno,
      @NotBlank @Size(max = 100) String apellidoMaterno,
      @NotBlank @Size(max = 20) String celular,
      @jakarta.validation.constraints.Email @Size(max = 254) String correoElectronico,
      @NotBlank @Pattern(regexp = "PADRE|MADRE|TUTOR_LEGAL|OTRO") String parentesco,
      boolean esPrincipal) {}
}
