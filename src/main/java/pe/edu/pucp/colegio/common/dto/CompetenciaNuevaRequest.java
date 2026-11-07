package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetenciaNuevaRequest(
    @NotBlank @Size(max = 150) String nombre, @Size(max = 500) String descripcion) {}
