package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;

public record ReglaConsolidacionResponse(
    String competenciaCodigo,
    BigDecimal valorC,
    BigDecimal valorB,
    BigDecimal valorA,
    BigDecimal valorAd,
    BigDecimal umbralB,
    BigDecimal umbralA,
    BigDecimal umbralAd) {}
