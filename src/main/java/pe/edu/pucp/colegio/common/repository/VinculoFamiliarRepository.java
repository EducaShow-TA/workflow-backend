package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;

public interface VinculoFamiliarRepository extends JpaRepository<VinculoFamiliar, Integer> {

  @EntityGraph(
      attributePaths = {
        "estudiante",
        "estudiante.persona",
        "estudiante.matriculas",
        "estudiante.matriculas.periodo",
        "estudiante.matriculas.periodo.grado",
        "estudiante.matriculas.periodo.seccion"
      })
  @Query(
      "select distinct v from VinculoFamiliar v "
          + "where v.apoderado.id = :apoderadoId and v.active = true "
          + "and v.apoderado.active = true and v.estudiante.active = true")
  List<VinculoFamiliar> listarEstudiantesActivosDeApoderado(
      @Param("apoderadoId") Integer apoderadoId);

  Optional<VinculoFamiliar> findByEstudianteIdAndApoderadoId(
      Integer estudianteId, Integer apoderadoId);

  List<VinculoFamiliar> findAllByEstudianteId(Integer estudianteId);

  java.util.Optional<VinculoFamiliar>
      findFirstByEstudianteIdAndEsPrincipalTrueAndActiveTrueOrderByIdAsc(Integer estudianteId);

  long countByEstudianteId(Integer estudianteId);

  boolean existsByEstudianteIdAndApoderadoIdAndActiveTrue(
      Integer estudianteId, Integer apoderadoId);

  boolean existsByEstudianteIdAndApoderadoIdAndActiveTrueAndApoderadoActiveTrue(
      Integer estudianteId, Integer apoderadoId);
}
