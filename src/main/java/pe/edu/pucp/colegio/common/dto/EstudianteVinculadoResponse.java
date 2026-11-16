package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.util.List;

/** Estudiante relacionado con el apoderado autenticado y sus períodos matriculados. */
public record EstudianteVinculadoResponse(
    Integer estudianteId, String nombreCompleto, List<PeriodoVinculado> periodos) {

  public record PeriodoVinculado(
      Integer periodoId,
      Integer gradoNumero,
      String seccion,
      LocalDate fechaInicio,
      LocalDate fechaFin) {}
}
