package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Apoderado;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Integer> {

  boolean existsByPersonaDni(String dni);

  Optional<Apoderado> findByPersonaDni(String dni);
}
