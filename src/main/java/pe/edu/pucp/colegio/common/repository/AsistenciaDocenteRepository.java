package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.AsistenciaDocente;

public interface AsistenciaDocenteRepository extends JpaRepository<AsistenciaDocente, Integer> {

  @EntityGraph(attributePaths = {"docente", "docente.persona"})
  Optional<AsistenciaDocente> findByIdAndActiveTrue(Integer id);

  @EntityGraph(attributePaths = {"docente", "docente.persona"})
  @Query(
      "select a from AsistenciaDocente a where a.active = true "
          + "and (:fecha is null or a.fecha = :fecha) "
          + "and (:docenteId is null or a.docente.id = :docenteId) "
          + "order by a.fecha desc, a.docente.id asc")
  List<AsistenciaDocente> buscar(
      @Param("fecha") LocalDate fecha, @Param("docenteId") Integer docenteId);

  boolean existsByDocenteIdAndFechaAndActiveTrue(Integer docenteId, LocalDate fecha);

  @EntityGraph(attributePaths = {"docente", "docente.persona"})
  Optional<AsistenciaDocente> findByDocenteIdAndFechaAndActiveTrue(
      Integer docenteId, LocalDate fecha);

  @EntityGraph(attributePaths = {"docente", "docente.persona"})
  @Query(
      "select a from AsistenciaDocente a where a.active = true and a.docente.id = :docenteId "
          + "and (:desde is null or a.fecha >= :desde) "
          + "and (:hasta is null or a.fecha <= :hasta) "
          + "order by a.fecha desc")
  List<AsistenciaDocente> buscarDelDocente(
      @Param("docenteId") Integer docenteId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);
}
