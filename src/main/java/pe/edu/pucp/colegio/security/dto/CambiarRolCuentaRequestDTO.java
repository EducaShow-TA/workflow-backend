package pe.edu.pucp.colegio.security.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.pucp.colegio.security.model.Rol;

public record CambiarRolCuentaRequestDTO(@NotNull Rol rol) {}
