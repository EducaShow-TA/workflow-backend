package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;

public record AsistenciaEstudianteRequest(
    @NotNull Integer estudianteId, @NotNull EstadoAsistencia estado) {}
