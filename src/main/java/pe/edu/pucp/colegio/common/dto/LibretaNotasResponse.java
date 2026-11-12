package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LibretaNotasResponse(
    Integer estudianteId,
    String estudiante,
    Integer periodoSolicitadoId,
    int anhioEscolar,
    Integer bimestre,
    int evaluacionesSinBimestre,
    List<PeriodoLibreta> periodos) {

  public record PeriodoLibreta(
      Integer periodoId,
      LocalDate fechaInicio,
      LocalDate fechaFin,
      Integer gradoNumero,
      String seccion,
      boolean actasCerradas,
      List<AreaLibreta> areas) {}

  public record AreaLibreta(String codigo, String nombre, List<CompetenciaLibreta> competencias) {}

  public record CompetenciaLibreta(
      String codigo,
      String nombre,
      String nota,
      BigDecimal puntajeConsolidado,
      String observacion,
      List<EvaluacionLibreta> evaluaciones) {}

  public record EvaluacionLibreta(
      Integer evaluacionId, String titulo, BigDecimal ponderacion, String nota, boolean justificada) {}
}
