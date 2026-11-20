package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.PeriodoCatalogoLibretaSnapshot;

public interface PeriodoCatalogoLibretaSnapshotRepository
    extends JpaRepository<PeriodoCatalogoLibretaSnapshot, Integer> {

  List<PeriodoCatalogoLibretaSnapshot> findAllByPeriodoIdOrderByAreaNombreAscCompetenciaNombreAsc(
      Integer periodoId);
}
