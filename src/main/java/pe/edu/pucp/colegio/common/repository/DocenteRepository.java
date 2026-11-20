package pe.edu.pucp.colegio.common.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Integer> {

  Optional<Docente> findByPersonaId(Integer personaId);

  Optional<Docente> findByPersonaDniAndActiveTrue(String dni);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from Docente d where d.id = :id")
  Optional<Docente> findByIdForUpdate(@Param("id") Integer id);
}
