package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Asistencia de varios alumnos de una sesión, guardada en una sola operación. */
public record RegistroAsistenciaSesionRequest(
    @NotEmpty List<@Valid AsistenciaEstudianteRequest> asistencias) {}
