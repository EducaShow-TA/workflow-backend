package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConfirmacionMasivaMatriculaRequest(@NotEmpty List<@NotNull Integer> estudianteIds) {}
