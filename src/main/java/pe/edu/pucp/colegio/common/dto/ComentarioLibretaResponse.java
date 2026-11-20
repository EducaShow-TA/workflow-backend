package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDateTime;

public record ComentarioLibretaResponse(
    Integer estudianteId,
    Integer periodoId,
    String comentario,
    String autorNombre,
    LocalDateTime actualizadoEn) {}
