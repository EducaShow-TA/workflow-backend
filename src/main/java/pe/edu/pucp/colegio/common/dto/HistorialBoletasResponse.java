package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.MecanismoGeneracion;

/** Todas las boletas de un estudiante, agrupadas por matrícula (US-1.4, US-1.5). */
public record HistorialBoletasResponse(
    Integer estudianteId,
    String estudiante,
    YearMonth mesCorte,
    int boletasGeneradas,
    int boletasEnMora,
    Totales totales,
    List<MatriculaHistorial> matriculas) {

  public record Totales(BigDecimal pagado, BigDecimal pendiente, BigDecimal enMora) {}

  public record MatriculaHistorial(
      Integer matriculaId,
      Integer anio,
      Integer grado,
      String seccion,
      LocalDate fechaMatricula,
      EstadoMatricula estado,
      CompromisoHistorial compromiso,
      List<BoletaHistorial> boletas) {}

  public record CompromisoHistorial(
      Integer id, String concepto, Integer numeroCuotas, YearMonth mesInicio) {}

  public record BoletaHistorial(
      Integer id,
      YearMonth mesCobro,
      LocalDate fechaVencimiento,
      BigDecimal montoTotal,
      EstadoBoleta estadoPago,
      MecanismoGeneracion mecanismoGeneracion,
      String numeroComprobante) {}
}
