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

  /** Secciones activas donde el docente tiene horario activo o dicta alguna sesión (US-2.6). */
  @Query(
      "select distinct s from Seccion s join fetch s.grado where s.active = true and ("
          + " exists (select sh.id from SesionHorario sh where sh.horario.seccion = s"
          + " and sh.horario.active = true and sh.docente.persona.id = :personaId)"
          + " or exists (select sc.id from SesionClase sc where sc.seccion = s"
          + " and sc.active = true and sc.docente.persona.id = :personaId))")
  List<Seccion> buscarSeccionesDelDocente(@Param("personaId") Integer personaId);
}
