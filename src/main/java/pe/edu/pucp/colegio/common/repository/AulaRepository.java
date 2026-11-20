package pe.edu.pucp.colegio.common.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.Aula;

public interface AulaRepository extends JpaRepository<Aula, Integer> {

  List<Aula> findAllByOrderByCodigoAsc();

  boolean existsByCodigoIgnoreCase(String codigo);

  boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Aula a where a.id = :id")
  Optional<Aula> findByIdForUpdate(@Param("id") Integer id);
}
