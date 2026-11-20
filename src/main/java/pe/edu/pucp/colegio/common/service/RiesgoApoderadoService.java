package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.AlertaRiesgoApoderadoResponse;
import pe.edu.pucp.colegio.common.dto.AlertaRiesgoApoderadoResponse.CompetenciaEnRiesgo;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.ConfiguracionRiesgoAcademicoRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

@Service
public class RiesgoApoderadoService {
  private final EstudiantesApoderadoService hijos;
  private final MatriculaRepository matriculas;
  private final AreaRepository areas;
  private final ConfiguracionRiesgoAcademicoRepository configuracion;
  private final ConteoRiesgoAcademicoService conteo;
  private final Clock clock;

  public RiesgoApoderadoService(
      EstudiantesApoderadoService hijos,
      MatriculaRepository matriculas,
      AreaRepository areas,
      ConfiguracionRiesgoAcademicoRepository configuracion,
      ConteoRiesgoAcademicoService conteo,
      Clock clock) {
    this.hijos = hijos;
    this.matriculas = matriculas;
    this.areas = areas;
    this.configuracion = configuracion;
    this.conteo = conteo;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AlertaRiesgoApoderadoResponse> listar(String username) {
    var ids = hijos.listar(username).stream().map(hijo -> hijo.estudianteId()).toList();
    if (ids.isEmpty()) return List.of();
    int umbral =
        configuracion
            .findById(1)
            .orElseThrow(() -> new IllegalStateException("Falta configurar el riesgo académico"))
            .getUmbral();
    var catalogo = areas.findAllByActivoTrueOrderByNombreAsc();
    var codigos =
        catalogo.stream()
            .flatMap(area -> area.getCompetencias().stream())
            .filter(competencia -> Boolean.TRUE.equals(competencia.getActivo()))
            .map(competencia -> competencia.getCodigo())
            .distinct()
            .toList();
    var hoy = LocalDate.now(clock);
    Map<Integer, Map<Integer, Map<String, Integer>>> porPeriodo = new HashMap<>();
    List<AlertaRiesgoApoderadoResponse> alertas = new ArrayList<>();
    for (var matricula : matriculas.findAllByEstudianteIdIn(ids)) {
      var periodo = matricula.getPeriodo();
      if (!Boolean.TRUE.equals(matricula.getActive())
          || matricula.getEstado() != EstadoMatricula.CONFIRMADA
          || !Boolean.TRUE.equals(periodo.getActive())
          || hoy.isBefore(periodo.getFechaInicio())
          || hoy.isAfter(periodo.getFechaFin())) continue;
      int bimestre = BimestreAcademico.actual(periodo, hoy);
      var notas =
          porPeriodo
              .computeIfAbsent(periodo.getId(), id -> conteo.contar(periodo, codigos, bimestre))
              .getOrDefault(matricula.getId(), Map.of());
      for (var area : catalogo) {
        var competencias =
            area.getCompetencias().stream()
                .filter(competencia -> Boolean.TRUE.equals(competencia.getActivo()))
                .filter(competencia -> notas.getOrDefault(competencia.getCodigo(), 0) > 0)
                .map(
                    competencia ->
                        new CompetenciaEnRiesgo(
                            competencia.getCodigo(),
                            competencia.getNombre(),
                            notas.get(competencia.getCodigo())))
                .sorted(java.util.Comparator.comparing(CompetenciaEnRiesgo::nombre))
                .toList();
        int cantidad = competencias.stream().mapToInt(CompetenciaEnRiesgo::notasC).sum();
        if (cantidad > umbral) {
          alertas.add(
              new AlertaRiesgoApoderadoResponse(
                  matricula.getEstudiante().getId(),
                  periodo.getId(),
                  bimestre,
                  area.getCodigo(),
                  area.getNombre(),
                  cantidad,
                  umbral,
                  competencias));
        }
      }
    }
    return alertas;
  }
}
