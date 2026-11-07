package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Aula;

public interface AulaRepository extends JpaRepository<Aula, Integer> {

    List<Aula> findAllByOrderByCodigoAsc();

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);
}
