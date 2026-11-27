package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PeriodoMatriculaRequest(
    LocalDate fechaInicio,
    LocalDate fechaLimite,
    Integer anhioEscolar,
    BigDecimal montoMatricula,
    BigDecimal montoPension) {

  public PeriodoMatriculaRequest(LocalDate fechaInicio, LocalDate fechaLimite) {
    this(fechaInicio, fechaLimite, null, null, null);
  }

  public PeriodoMatriculaRequest(
      LocalDate fechaInicio, LocalDate fechaLimite, Integer anhioEscolar) {
    this(fechaInicio, fechaLimite, anhioEscolar, null, null);
  }
}
