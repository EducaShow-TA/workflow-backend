package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CalificacionParcialRequest(
    @Pattern(regexp = "AD|A|B|C", message = "La nota debe ser AD, A, B o C") String nota,
    @NotNull Boolean justificada,
    @Size(max = 2048) String evidenciaUrl) {}
