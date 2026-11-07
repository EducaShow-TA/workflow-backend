package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;

public record AsistenciaDocenteAjusteRequest(
    @NotNull EstadoAsistenciaDocente estado, @NotBlank @Size(max = 500) String motivoAjuste) {}
