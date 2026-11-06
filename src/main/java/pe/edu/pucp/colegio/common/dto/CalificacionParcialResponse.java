package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDateTime;

public record CalificacionParcialResponse(
    Integer id,
    Integer matriculaId,
    Integer estudianteId,
    String estudiante,
    String nota,
    boolean justificada,
    String evidenciaTipo,
    String evidenciaNombre,
    String evidenciaUrl,
    Long evidenciaTamanio,
    LocalDateTime fechaRegistro) {}
