package pe.edu.pucp.colegio.common.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.CatalogoLibretaSnapshot;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Competencia;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.PeriodoCatalogoLibretaSnapshot;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoCatalogoLibretaSnapshotRepository;

@Service
public class CatalogoLibretaSnapshotService {

  private final AreaRepository areas;
  private final PeriodoCatalogoLibretaSnapshotRepository snapshots;

  public CatalogoLibretaSnapshotService(
      AreaRepository areas, PeriodoCatalogoLibretaSnapshotRepository snapshots) {
    this.areas = areas;
    this.snapshots = snapshots;
  }

  @Transactional
  public void capturar(Periodo periodo) {
    List<PeriodoCatalogoLibretaSnapshot> filas =
        areas.findAllByActivoTrueOrderByNombreAsc().stream()
            .flatMap(
                area ->
                    area.getCompetencias().stream()
                        .filter(competencia -> Boolean.TRUE.equals(competencia.getActivo()))
                        .map(competencia -> fila(periodo, area, competencia)))
            .toList();
    snapshots.saveAll(filas);
    periodo.setCatalogoLibretaSnapshot(true);
  }

  @Transactional(readOnly = true)
  public List<CatalogoLibretaSnapshot> obtener(Periodo periodo) {
    if (!Boolean.TRUE.equals(periodo.getCatalogoLibretaSnapshot())) {
      return areas.findAllByActivoTrueOrderByNombreAsc().stream()
          .map(
              area ->
                  new CatalogoLibretaSnapshot(
                      area.getCodigo(),
                      area.getNombre(),
                      area.getCompetencias().stream()
                          .filter(competencia -> Boolean.TRUE.equals(competencia.getActivo()))
                          .map(
                              competencia ->
                                  new CatalogoLibretaSnapshot.Competencia(
                                      competencia.getCodigo(), competencia.getNombre()))
                          .toList()))
          .toList();
    }

    Map<String, CatalogoLibretaSnapshot> porArea = new LinkedHashMap<>();
    for (PeriodoCatalogoLibretaSnapshot fila :
        snapshots.findAllByPeriodoIdOrderByAreaNombreAscCompetenciaNombreAsc(periodo.getId())) {
      CatalogoLibretaSnapshot area =
          porArea.computeIfAbsent(
              fila.getAreaCodigo(),
              codigo ->
                  new CatalogoLibretaSnapshot(codigo, fila.getAreaNombre(), new ArrayList<>()));
      area.competencias()
          .add(
              new CatalogoLibretaSnapshot.Competencia(
                  fila.getCompetenciaCodigo(), fila.getCompetenciaNombre()));
    }
    return List.copyOf(porArea.values());
  }

  private PeriodoCatalogoLibretaSnapshot fila(Periodo periodo, Area area, Competencia competencia) {
    return new PeriodoCatalogoLibretaSnapshot(
        periodo,
        area.getCodigo(),
        area.getNombre(),
        competencia.getCodigo(),
        competencia.getNombre());
  }
}
