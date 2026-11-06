package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.ReglaConsolidacion;

public interface ReglaConsolidacionRepository extends JpaRepository<ReglaConsolidacion, String> {
  Optional<ReglaConsolidacion> findByCompetenciaCodigo(String competenciaCodigo);
}
