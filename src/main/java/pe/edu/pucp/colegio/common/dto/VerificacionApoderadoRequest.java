package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Documento de la persona que se quiere vincular como apoderado; viaja en el cuerpo, no en la URL.
 */
public record VerificacionApoderadoRequest(
    @NotBlank @Pattern(regexp = "DNI|PASAPORTE") String tipoDocumento,
    @NotBlank @Size(max = 15) String numeroDocumento) {}
