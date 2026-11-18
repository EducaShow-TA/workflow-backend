package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;

/** Estudiante con sus períodos de matrícula confirmada, del más reciente al más antiguo. */
public record EstudianteVinculadoResponse(
    Integer estudianteId, String nombreCompleto, List<PeriodoVinculado> periodos) {

  public static EstudianteVinculadoResponse desde(Estudiante estudiante) {
    var periodos =
        estudiante.getMatriculas().stream()
            .filter(matricula -> Boolean.TRUE.equals(matricula.getActive()))
            .filter(matricula -> matricula.getEstado() == EstadoMatricula.CONFIRMADA)
            .map(
                matricula -> {
                  var periodo = matricula.getPeriodo();
                  return new PeriodoVinculado(
                      periodo.getId(),
                      periodo.getGrado().getNumero(),
                      periodo.getSeccion().getCodigo(),
                      periodo.getFechaInicio(),
                      periodo.getFechaFin());
                })
            .sorted(Comparator.comparing(PeriodoVinculado::fechaInicio).reversed())
            .toList();
    return new EstudianteVinculadoResponse(
        estudiante.getId(), estudiante.getPersona().getNombreCompleto(), periodos);
  }

  public record PeriodoVinculado(
      Integer periodoId,
      Integer gradoNumero,
      String seccion,
      LocalDate fechaInicio,
      LocalDate fechaFin) {}
}
