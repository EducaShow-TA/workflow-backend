package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.edu.pucp.colegio.common.model.MedioPago;

public record ComprobantePagoResponse(
    Integer id,
    Integer boletaId,
    LocalDate fechaEmision,
    BigDecimal montoPagado,
    MedioPago medioPago,
    String numeroComprobante) {}
