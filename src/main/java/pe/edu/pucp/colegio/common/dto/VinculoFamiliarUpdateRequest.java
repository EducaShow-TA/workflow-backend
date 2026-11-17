package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.pucp.colegio.common.validation.TelefonoPolicy;

public record VinculoFamiliarUpdateRequest(
    @NotBlank @Pattern(regexp = "PADRE|MADRE|TUTOR_LEGAL|OTRO") String parentesco,
    boolean esPrincipal,
    @Size(max = 100) String nombres,
    @Size(max = 100) String apellidoPaterno,
    @Size(max = 100) String apellidoMaterno,
    @Pattern(regexp = TelefonoPolicy.PATRON, message = "El celular debe usar formato internacional")
        @Size(max = 20)
        String celular,
    @Email @Size(max = 254) String correoElectronico) {}
