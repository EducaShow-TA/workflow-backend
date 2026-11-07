package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;

public interface GradoRepository extends JpaRepository<Grado, Integer> {

  Optional<Grado> findByNumeroAndNivelEducativoAndActiveTrue(
      Integer numero, NivelEducativo nivelEducativo);
}
