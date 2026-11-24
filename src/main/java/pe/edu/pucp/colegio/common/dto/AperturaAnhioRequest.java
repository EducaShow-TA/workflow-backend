package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record AperturaAnhioRequest(
    @NotNull @Min(2000) @Max(2100) Integer anhio,
    @NotNull LocalDate fechaInicio,
    @NotNull LocalDate fechaFin,
    @Valid List<BimestreFechasRequest> bimestres) {

  public AperturaAnhioRequest(Integer anhio, LocalDate fechaInicio, LocalDate fechaFin) {
    this(anhio, fechaInicio, fechaFin, null);
  }
}
