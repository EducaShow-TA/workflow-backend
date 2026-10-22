package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record AreaCatalogoCreateRequest(
    @Size(max = 50) String codigo,
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 500) String descripcion,
    Set<String> competenciaCodigos,
    List<@Valid CompetenciaNuevaRequest> competencias) {}
