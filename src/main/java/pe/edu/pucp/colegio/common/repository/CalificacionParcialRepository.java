package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.CalificacionParcial;

public interface CalificacionParcialRepository extends JpaRepository<CalificacionParcial, Integer> {

  Optional<CalificacionParcial> findByEvaluacionIdAndMatriculaIdAndActiveTrue(
      Integer evaluacionId, Integer matriculaId);

  Optional<CalificacionParcial> findByIdAndActiveTrue(Integer id);

  @EntityGraph(
      attributePaths = {"matricula", "matricula.estudiante", "matricula.estudiante.persona"})
  List<CalificacionParcial> findAllByEvaluacionIdAndActiveTrueOrderByMatriculaId(
      Integer evaluacionId);

  @EntityGraph(attributePaths = {"evaluacion", "matricula"})
  List<CalificacionParcial> findAllByEvaluacionIdInAndActiveTrue(List<Integer> evaluacionIds);
}
