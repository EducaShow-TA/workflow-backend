package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;

public interface VinculoFamiliarRepository extends JpaRepository<VinculoFamiliar, Integer> {

  Optional<VinculoFamiliar> findByEstudianteIdAndApoderadoId(
      Integer estudianteId, Integer apoderadoId);

  List<VinculoFamiliar> findAllByEstudianteId(Integer estudianteId);

  long countByEstudianteId(Integer estudianteId);
}
