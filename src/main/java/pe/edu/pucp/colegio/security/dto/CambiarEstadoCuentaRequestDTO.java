package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoCuentaRequestDTO(@NotNull Boolean activo) {}
