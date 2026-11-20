package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CuadroMeritoResponse(
    Integer periodoId,
    int anhioEscolar,
    int gradoNumero,
    String nivelEducativo,
    String seccion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    List<EstudianteRanking> estudiantes) {

  public record EstudianteRanking(
      Integer puesto,
      Integer matriculaId,
      Integer estudianteId,
      String estudiante,
      String seccion,
      BigDecimal promedio,
      int competenciasCalificadas) {}
}
