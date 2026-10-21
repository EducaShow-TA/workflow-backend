package pe.edu.pucp.colegio.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

  boolean existsByPersonaId(Integer personaId);
}
