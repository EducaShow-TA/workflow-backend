package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Competencia;

public interface CompetenciaRepository extends JpaRepository<Competencia, String> {

  @EntityGraph(attributePaths = "areas")
  List<Competencia> findAllByCodigoIn(List<String> codigos);

  Optional<Competencia> findByIdCatalogo(Integer idCatalogo);

  boolean existsByCodigo(String codigo);
}
