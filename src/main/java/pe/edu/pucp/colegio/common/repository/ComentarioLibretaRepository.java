package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.ComentarioLibreta;

public interface ComentarioLibretaRepository extends JpaRepository<ComentarioLibreta, Integer> {
  Optional<ComentarioLibreta> findByMatriculaIdAndActiveTrue(Integer matriculaId);
}
