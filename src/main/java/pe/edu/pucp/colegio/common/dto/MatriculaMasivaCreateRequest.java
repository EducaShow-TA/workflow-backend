package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MatriculaMasivaCreateRequest(@NotEmpty List<@NotNull Integer> estudianteIds) {}
