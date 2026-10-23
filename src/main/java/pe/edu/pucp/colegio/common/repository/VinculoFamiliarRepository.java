package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;

public interface VinculoFamiliarRepository extends JpaRepository<VinculoFamiliar, Integer> {

  Optional<VinculoFamiliar> findByEstudianteId(Integer estudianteId);
}
