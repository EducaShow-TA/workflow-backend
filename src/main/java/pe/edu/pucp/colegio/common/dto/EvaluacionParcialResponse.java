package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;

public record EvaluacionParcialResponse(
    Integer id,
    Integer periodoId,
    String competenciaCodigo,
    String titulo,
    String descripcion,
    BigDecimal ponderacion,
    Integer bimestre) {}
