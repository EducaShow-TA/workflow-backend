package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, String> {

  Optional<Competencia> findByIdCatalogo(Integer idCatalogo);

  boolean existsByCodigo(String codigo);
}
