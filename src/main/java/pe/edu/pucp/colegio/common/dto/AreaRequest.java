package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Size;
import java.util.Set;

public record AreaRequest(
    @Size(max = 50) String codigo,
    @jakarta.validation.constraints.NotBlank @Size(max = 150) String nombre,
    @Size(max = 500) String descripcion,
    Set<String> competenciaCodigos) {}
