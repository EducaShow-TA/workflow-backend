package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CompetenciaRequest(
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 255) String nombre,
    @Size(max = 255) String descripcion,
    Set<String> areaCodigos) {}
