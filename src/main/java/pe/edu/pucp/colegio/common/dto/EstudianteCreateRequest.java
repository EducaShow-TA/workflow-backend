package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import pe.edu.pucp.colegio.common.validation.PasswordPolicy;
import pe.edu.pucp.colegio.common.validation.TelefonoPolicy;

public record EstudianteCreateRequest(
    @NotBlank @PasswordPolicy String password,
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

  /** Compatibilidad de constructores Java: el usuario no se recibe ni se usa. */
  public EstudianteCreateRequest(
      String usernameIgnorado,
      String password,
      String tipoDocumento,
      String numeroDocumento,
      String nombres,
      String apellidoPaterno,
      String apellidoMaterno,
      LocalDate fechaNacimiento,
      String celular,
      String genero,
      String correoElectronico,
      String nacionalidad,
      String direccion,
      List<ApoderadoCreateRequest> apoderados) {
    this(
        password,
        tipoDocumento,
        numeroDocumento,
        nombres,
        apellidoPaterno,
        apellidoMaterno,
        fechaNacimiento,
        celular,
        genero,
        correoElectronico,
        nacionalidad,
        direccion,
        apoderados);
  }

  public record ApoderadoCreateRequest(
      @NotBlank @Pattern(regexp = "DNI|PASAPORTE") String tipoDocumento,
      @NotBlank @Size(max = 15) String numeroDocumento,
      @NotBlank @Size(max = 100) String nombres,
      @NotBlank @Size(max = 100) String apellidoPaterno,
      @NotBlank @Size(max = 100) String apellidoMaterno,
      @NotBlank
          @Pattern(
              regexp = TelefonoPolicy.PATRON,
              message = "El celular debe usar formato internacional")
          @Size(max = 20)
          String celular,
      @jakarta.validation.constraints.Email @Size(max = 254) String correoElectronico,
      @NotBlank @Pattern(regexp = "PADRE|MADRE|TUTOR_LEGAL|OTRO") String parentesco,
      boolean esPrincipal) {}
}
