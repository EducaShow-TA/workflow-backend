package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.MecanismoGeneracion;
import pe.edu.pucp.colegio.common.model.TipoCargoBoleta;

/** El campo JSON {@code mora} es el recargo monetario, separado de {@code estadoPago}. */
public record BoletaPensionResponse(
    Integer id,
    Integer compromisoId,
    Integer matriculaId,
    Integer estudianteId,
    String estudiante,
    Integer apoderadoId,
    String apoderado,
    Integer grado,
    String seccion,
    YearMonth mesCobro,
    BigDecimal monto,
    BigDecimal mora,
    BigDecimal montoTotal,
    LocalDate fechaVencimiento,
    EstadoBoleta estadoPago,
    TipoCargoBoleta tipoCargo,
    MecanismoGeneracion mecanismoGeneracion,
    LocalDateTime fechaGeneracion) {}
