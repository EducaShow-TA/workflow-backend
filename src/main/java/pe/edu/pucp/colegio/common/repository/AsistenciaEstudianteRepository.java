package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;

public interface AsistenciaEstudianteRepository
    extends JpaRepository<AsistenciaEstudiante, Integer> {

  @EntityGraph(
      attributePaths = {"estudiante", "estudiante.persona", "sesionClase", "sesionClase.seccion"})
  Optional<AsistenciaEstudiante> findByIdAndActiveTrue(Integer id);

  @EntityGraph(attributePaths = {"estudiante", "estudiante.persona"})
  List<AsistenciaEstudiante> findAllBySesionClaseIdAndActiveTrueOrderByEstudianteId(
      Integer sesionClaseId);

  @EntityGraph(attributePaths = {"estudiante", "estudiante.persona", "sesionClase"})
  Optional<AsistenciaEstudiante> findBySesionClaseIdAndEstudianteIdAndActiveTrue(
      Integer sesionClaseId, Integer estudianteId);

  boolean existsBySesionClaseIdAndEstudianteIdAndActiveTrue(
      Integer sesionClaseId, Integer estudianteId);

  @EntityGraph(
      attributePaths = {
        "sesionClase",
        "sesionClase.seccion",
        "sesionClase.area",
        "sesionClase.docente",
        "sesionClase.docente.persona"
      })
  @Query(
      "select a from AsistenciaEstudiante a where a.active = true "
          + "and a.estudiante.id = :estudianteId and a.sesionClase.active = true "
          + "and (:desde is null or a.sesionClase.fecha >= :desde) "
          + "and (:hasta is null or a.sesionClase.fecha <= :hasta) "
          + "order by a.sesionClase.fecha desc, a.sesionClase.horaInicio desc")
  List<AsistenciaEstudiante> buscarHistorial(
      @Param("estudianteId") Integer estudianteId,
      @Param("desde") java.time.LocalDate desde,
      @Param("hasta") java.time.LocalDate hasta);
}
