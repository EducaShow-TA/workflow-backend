package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoEntregaRequest(@NotNull Boolean cumplida) {}
