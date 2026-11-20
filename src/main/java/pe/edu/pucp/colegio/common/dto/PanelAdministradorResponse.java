package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.colegio.common.model.EstadoPeriodoMatricula;

public record PanelAdministradorResponse(
    LocalDate fecha,
    int anio,
    VentanaMatricula ventanaMatricula,
    Pendientes pendientes,
    Indicadores indicadores,
    List<CobranzaMes> cobranza,
    List<OcupacionGrado> ocupacion,
    List<Actividad> actividad) {

  public record VentanaMatricula(
      LocalDate fechaInicio, LocalDate fechaLimite, EstadoPeriodoMatricula estado) {}

  public record Pendientes(
      long matriculasPorConfirmar,
      long familiasEnMora,
      BigDecimal montoEnMora,
      long docentesSinMarcar,
      long erroresUltimas24h,
      long erroresCriticos24h) {}

  public record Indicadores(
      long estudiantesMatriculados,
      long vacantesTotales,
      BigDecimal pensionMesEmitida,
      BigDecimal pensionMesCobrada,
      long asistenciasSemana,
      long presentesSemana,
      long tardanzasSemana,
      long docentesConClaseHoy,
      long docentesPresentesHoy,
      long docentesTardanzaHoy) {}

  public record CobranzaMes(int mes, BigDecimal pagado, BigDecimal vencido, BigDecimal porVencer) {}

  public record OcupacionGrado(
      String nivelEducativo, int grado, int secciones, long matriculados, long vacantes) {}

  public record Actividad(String tipo, String titulo, String detalle, LocalDateTime fecha) {}
}
