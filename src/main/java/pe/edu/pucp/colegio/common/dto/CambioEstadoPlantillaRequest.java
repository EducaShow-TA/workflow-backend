package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;

public record CambioEstadoPlantillaRequest(@NotNull Boolean activo) {}
