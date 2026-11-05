package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;

public record AsistenciaEstudianteAjusteRequest(@NotNull EstadoAsistencia estado) {}
