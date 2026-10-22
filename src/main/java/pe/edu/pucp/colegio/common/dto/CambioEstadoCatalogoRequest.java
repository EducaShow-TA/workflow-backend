package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoCatalogoRequest(@NotNull Boolean activo) {}
