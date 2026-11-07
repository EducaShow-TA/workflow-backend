package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Integer> {

  Optional<Docente> findByPersonaId(Integer personaId);
}
