package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.CalificacionParcial;

public interface CalificacionParcialRepository extends JpaRepository<CalificacionParcial, Integer> {

  @Query(
      "select case when count(c) > 0 then true else false end "
          + "from CalificacionParcial c where c.evaluacion.competencia.codigo = :codigo")
  boolean existsByCompetenciaCodigo(@Param("codigo") String competenciaCodigo);

  Optional<CalificacionParcial> findByEvaluacionIdAndMatriculaIdAndActiveTrue(
      Integer evaluacionId, Integer matriculaId);

  Optional<CalificacionParcial> findByIdAndActiveTrue(Integer id);

  @EntityGraph(
      attributePaths = {"matricula", "matricula.estudiante", "matricula.estudiante.persona"})
  List<CalificacionParcial> findAllByEvaluacionIdAndActiveTrueOrderByMatriculaId(
      Integer evaluacionId);

  @EntityGraph(attributePaths = {"evaluacion", "matricula"})
  List<CalificacionParcial> findAllByEvaluacionIdInAndActiveTrue(List<Integer> evaluacionIds);

  @EntityGraph(attributePaths = {"evaluacion"})
  List<CalificacionParcial> findAllByEvaluacionIdInAndMatriculaIdAndActiveTrue(
      List<Integer> evaluacionIds, Integer matriculaId);
}
