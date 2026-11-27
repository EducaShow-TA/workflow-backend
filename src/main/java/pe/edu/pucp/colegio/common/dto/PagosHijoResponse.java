package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.MedioPago;
import pe.edu.pucp.colegio.common.model.TipoCargoBoleta;

/** Compromisos de pago y boletas emitidas de un hijo del apoderado autenticado (HU 1.8). */
public record PagosHijoResponse(
    Integer estudianteId, String nombreCompleto, List<CompromisoApoderado> compromisos) {

  public enum Situacion {
    AL_DIA,
    CON_PENDIENTES,
    CON_MORA
  }

  public record CompromisoApoderado(
      Integer compromisoId,
      String concepto,
      Integer anio,
      Integer gradoNumero,
      String seccion,
      String apoderadoTitular,
      BigDecimal montoTotal,
      BigDecimal montoPagado,
      BigDecimal montoPorPagar,
      Integer numeroCuotas,
      long cuotasPagadas,
      long cuotasEnMora,
      long cuotasPendientes,
      int cuotasPorEmitir,
      LocalDate proximaEmision,
      Situacion situacion,
      List<BoletaApoderado> boletas) {}

  public record BoletaApoderado(
      Integer boletaId,
      YearMonth mesCobro,
      LocalDate fechaVencimiento,
      BigDecimal monto,
      BigDecimal mora,
      BigDecimal montoTotal,
      EstadoBoleta estadoPago,
      TipoCargoBoleta tipoCargo,
      ComprobanteApoderado comprobante) {}

  public record ComprobanteApoderado(
      Integer comprobanteId,
      String numeroComprobante,
      LocalDate fechaEmision,
      MedioPago medioPago,
      BigDecimal montoPagado) {}
}
