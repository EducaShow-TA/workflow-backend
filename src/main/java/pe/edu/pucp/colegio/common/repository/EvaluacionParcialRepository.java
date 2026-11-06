package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.EvaluacionParcial;

public interface EvaluacionParcialRepository extends JpaRepository<EvaluacionParcial, Integer> {

  boolean existsByPeriodoIdAndCompetenciaCodigoAndActiveTrue(
      Integer periodoId, String competenciaCodigo);

  List<EvaluacionParcial> findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrueOrderById(
      Integer periodoId, String competenciaCodigo);

  List<EvaluacionParcial> findAllByPeriodoIdAndActiveTrueOrderById(Integer periodoId);

  @Query(
      "select distinct e.competencia.codigo from EvaluacionParcial e "
          + "where e.periodo.id = :periodoId and e.active = true")
  List<String> findDistinctCompetenciaCodigosByPeriodoId(@Param("periodoId") Integer periodoId);

  @Query(
      "select distinct e.periodo.id from EvaluacionParcial e "
          + "where e.competencia.codigo = :codigo and e.active = true")
  List<Integer> findDistinctPeriodoIdsByCompetenciaCodigo(@Param("codigo") String codigo);

  List<EvaluacionParcial> findAllByIdInAndActiveTrueOrderById(List<Integer> ids);
}
