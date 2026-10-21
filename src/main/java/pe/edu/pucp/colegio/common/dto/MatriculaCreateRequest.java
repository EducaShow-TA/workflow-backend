package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;

public record MatriculaCreateRequest(@NotNull Integer estudianteId) {}
