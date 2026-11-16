package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.EntregaTarea;

public interface EntregaTareaRepository extends JpaRepository<EntregaTarea, Integer> {
  @EntityGraph(
      attributePaths = {"matricula", "matricula.estudiante", "matricula.estudiante.persona"})
  List<EntregaTarea> findAllByTareaIdAndActiveTrueOrderByMatriculaId(Integer tareaId);

  Optional<EntregaTarea> findByTareaIdAndMatriculaIdAndActiveTrue(
      Integer tareaId, Integer matriculaId);
}
