package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Seccion;

public interface SeccionRepository extends JpaRepository<Seccion, Integer> {

  List<Seccion> findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc();

  boolean existsByGradoIdAndCodigoIgnoreCase(Integer gradoId, String codigo);
}
