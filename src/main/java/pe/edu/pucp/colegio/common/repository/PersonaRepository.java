package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Persona;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {

  boolean existsByDni(String dni);

  Optional<Persona> findByDni(String dni);
}
