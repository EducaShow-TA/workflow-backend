package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SeccionVacantesRequest(@NotNull @Positive Integer vacantesTotales) {}
