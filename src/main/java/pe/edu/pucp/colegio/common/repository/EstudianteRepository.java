package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

  @EntityGraph(
      attributePaths = {
        "persona",
        "matriculas",
        "matriculas.periodo",
        "matriculas.periodo.seccion",
        "matriculas.periodo.grado"
      })
  List<Estudiante> findAllByActiveTrueOrderByIdAsc();

  boolean existsByPersonaId(Integer personaId);
}
