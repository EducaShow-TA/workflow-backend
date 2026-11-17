package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import pe.edu.pucp.colegio.common.model.Periodo;

/** Divide las fechas lectivas de un período en cuatro tramos consecutivos de duración similar. */
public final class BimestreAcademico {
  private BimestreAcademico() {}

  public static int actual(Periodo periodo, LocalDate fecha) {
    LocalDate inicio = periodo.getFechaInicio();
    LocalDate fin = periodo.getFechaFin();
    long diasTotales = ChronoUnit.DAYS.between(inicio, fin) + 1;
    long dia = Math.max(0, Math.min(diasTotales - 1, ChronoUnit.DAYS.between(inicio, fecha)));
    return Math.min(4, Math.toIntExact(dia * 4 / diasTotales) + 1);
  }
}
