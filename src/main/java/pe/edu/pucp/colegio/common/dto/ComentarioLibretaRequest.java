package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioLibretaRequest(@NotBlank @Size(max = 2000) String comentario) {}
