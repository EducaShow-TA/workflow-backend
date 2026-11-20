package pe.edu.pucp.colegio.common.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;

public interface HorarioSeccionRepository extends JpaRepository<HorarioSeccion, Integer> {

  Optional<HorarioSeccion> findBySeccionIdAndAnio(Integer seccionId, Integer anio);

  boolean existsBySeccionIdAndAnio(Integer seccionId, Integer anio);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select h from HorarioSeccion h where h.id = :id")
  Optional<HorarioSeccion> findByIdForUpdate(@Param("id") Integer id);
}
