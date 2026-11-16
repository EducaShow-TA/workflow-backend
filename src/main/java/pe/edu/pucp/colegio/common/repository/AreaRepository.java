package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Area;

public interface AreaRepository extends JpaRepository<Area, String> {

  Optional<Area> findByIdCatalogo(Integer idCatalogo);

  boolean existsByCodigo(String codigo);

  @EntityGraph(attributePaths = "competencias")
  List<Area> findAllByActivoTrueOrderByNombreAsc();
}
