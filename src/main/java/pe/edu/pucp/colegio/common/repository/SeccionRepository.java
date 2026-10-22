package pe.edu.pucp.colegio.common.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.Seccion;

public interface SeccionRepository extends JpaRepository<Seccion, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Seccion s where s.id = :id")
  Optional<Seccion> findByIdForUpdate(@Param("id") Integer id);

  List<Seccion> findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc();

  boolean existsByGradoIdAndCodigoIgnoreCase(Integer gradoId, String codigo);
}
