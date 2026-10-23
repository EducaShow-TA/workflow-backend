package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;

public interface PeriodoMatriculaRepository extends JpaRepository<PeriodoMatricula, Integer> {

  List<PeriodoMatricula> findAllByActiveTrueOrderByFechaInicioDesc();

  boolean existsByActiveTrueAndFechaInicioLessThanEqualAndFechaLimiteGreaterThanEqual(
      LocalDate inicioHasta, LocalDate limiteDesde);
}
