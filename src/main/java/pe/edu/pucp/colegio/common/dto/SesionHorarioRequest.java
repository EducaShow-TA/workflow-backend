package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record SesionHorarioRequest(
    @NotNull Integer bloqueId,
    @NotNull DayOfWeek dia,
    @NotBlank String areaCodigo,
    @NotNull Integer docenteId,
    @NotNull Integer aulaId,
    String tipoVigencia,
    LocalDate vigenteDesde,
    LocalDate vigenteHasta) {
  public SesionHorarioRequest(
      Integer bloqueId, DayOfWeek dia, String areaCodigo, Integer docenteId, Integer aulaId) {
    this(bloqueId, dia, areaCodigo, docenteId, aulaId, "PERMANENTE", null, null);
  }
}
