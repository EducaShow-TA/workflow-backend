package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;

public interface PeriodoMatriculaRepository extends JpaRepository<PeriodoMatricula, Integer> {

  List<PeriodoMatricula> findAllByActiveTrueOrderByFechaInicioDesc();

  boolean existsByActiveTrueAndFechaInicioLessThanEqualAndFechaLimiteGreaterThanEqual(
      LocalDate inicioHasta, LocalDate limiteDesde);

  long countByActiveTrue();

  @Query(
      "select p from PeriodoMatricula p where p.active = true"
          + " and (:idExcluido is null or p.id <> :idExcluido)"
          + " and p.fechaInicio <= :fechaLimite and p.fechaLimite >= :fechaInicio"
          + " order by p.fechaInicio")
  List<PeriodoMatricula> buscarCruces(
      @Param("fechaInicio") LocalDate fechaInicio,
      @Param("fechaLimite") LocalDate fechaLimite,
      @Param("idExcluido") Integer idExcluido);
}
