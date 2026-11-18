package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;

/** Totales de los eventos que cumplen los filtros, sin depender de la página. */
public record AuditoriaFinancieraResumenResponse(
    long eventos,
    long pagos,
    BigDecimal montoPagado,
    long boletasEnMora,
    long boletasGeneradas,
    long compromisosGenerados) {}
