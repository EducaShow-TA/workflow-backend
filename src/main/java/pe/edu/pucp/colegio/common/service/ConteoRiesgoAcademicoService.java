package pe.edu.pucp.colegio.common.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.CalificacionParcialRepository;
import pe.edu.pucp.colegio.common.repository.CalificacionRepository;
import pe.edu.pucp.colegio.common.repository.EvaluacionParcialRepository;

/** Cuenta notas C por matrícula y competencia con la misma regla para docentes y familias. */
@Service
public class ConteoRiesgoAcademicoService {
  private final CalificacionRepository calificaciones;
  private final CalificacionParcialRepository parciales;
  private final EvaluacionParcialRepository evaluaciones;
  private final CalendarioBimestralService calendario;

  public ConteoRiesgoAcademicoService(
      CalificacionRepository calificaciones,
      CalificacionParcialRepository parciales,
      EvaluacionParcialRepository evaluaciones,
      CalendarioBimestralService calendario) {
    this.calificaciones = calificaciones;
    this.parciales = parciales;
    this.evaluaciones = evaluaciones;
    this.calendario = calendario;
  }

  @Transactional(readOnly = true)
  public Map<Integer, Map<String, Integer>> contar(
      Periodo periodo, List<String> competencias, int bimestre) {
    Map<Integer, Map<String, Integer>> conteos = new HashMap<>();
    if (competencias.isEmpty()) return conteos;
    var bimestreDe = calendario.calculadora(periodo);
    for (var nota :
        calificaciones.findAllByPeriodoIdAndCompetenciaCodigoInAndActiveTrue(
            periodo.getId(), competencias)) {
      int bimestreNota =
          nota.getBimestre() == null
              ? bimestreDe.applyAsInt(nota.getFechaRegistro().toLocalDate())
              : nota.getBimestre();
      if (!Boolean.TRUE.equals(nota.getConsolidada())
          && bimestre == bimestreNota
          && "C".equalsIgnoreCase(nota.getNota())) {
        conteos
            .computeIfAbsent(nota.getMatricula().getId(), id -> new HashMap<>())
            .merge(nota.getCompetencia().getCodigo(), 1, Integer::sum);
      }
    }
    var ids =
        evaluaciones
            .findAllByPeriodoIdAndBimestreAndActiveTrueOrderById(periodo.getId(), bimestre)
            .stream()
            .filter(e -> competencias.contains(e.getCompetencia().getCodigo()))
            .map(e -> e.getId())
            .toList();
    if (!ids.isEmpty()) {
      for (var nota : parciales.findAllByEvaluacionIdInAndActiveTrue(ids)) {
        if (!Boolean.TRUE.equals(nota.getJustificada()) && "C".equalsIgnoreCase(nota.getNota())) {
          conteos
              .computeIfAbsent(nota.getMatricula().getId(), id -> new HashMap<>())
              .merge(nota.getEvaluacion().getCompetencia().getCodigo(), 1, Integer::sum);
        }
      }
    }
    return conteos;
  }
}
