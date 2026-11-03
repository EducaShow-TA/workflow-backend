package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.util.List;

public record BoletaPensionReporteResponse(
    List<BoletaPensionResponse> boletas, BigDecimal montoPendiente, long familiasMorosas) {}
