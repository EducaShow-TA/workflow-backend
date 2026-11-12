package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record CompromisoPagoResponse(
    Integer id,
    Integer matriculaId,
    Integer estudianteId,
    Integer apoderadoId,
    String concepto,
    BigDecimal montoTotal,
    LocalDate fechaEmision,
    YearMonth mesInicio,
    Integer numeroCuotas,
    LocalDate primerVencimiento,
    Integer diaVencimiento) {}
