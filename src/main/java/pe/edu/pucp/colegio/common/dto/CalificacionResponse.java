package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CalificacionResponse(
    Integer id,
    Integer matriculaId,
    Integer estudianteId,
    String estudiante,
    String competenciaCodigo,
    String nota,
    String observacion,
    String evidenciaTipo,
    String evidenciaNombre,
    String evidenciaUrl,
    Long evidenciaTamanio,
    BigDecimal puntajeConsolidado,
    boolean consolidada,
    LocalDateTime fechaRegistro) {}
