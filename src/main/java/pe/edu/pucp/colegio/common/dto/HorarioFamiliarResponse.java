package pe.edu.pucp.colegio.common.dto;

import java.time.DayOfWeek;
import java.util.List;

/** US-4.7: horario de solo lectura de la sección del estudiante autorizado. */
public record HorarioFamiliarResponse(
    Integer estudianteId,
    String estudianteNombre,
    Integer gradoNumero,
    String seccionCodigo,
    Integer anio,
    boolean horarioDisponible,
    List<Bloque> bloques) {

  public record Bloque(
      Integer id,
      DayOfWeek dia,
      String horaInicio,
      String horaFin,
      String areaNombre,
      String aulaCodigo,
      String docenteNombre) {}
}
