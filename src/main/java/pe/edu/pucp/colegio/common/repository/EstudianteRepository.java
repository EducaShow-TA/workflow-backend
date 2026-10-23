package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

  @EntityGraph(
      attributePaths = {
        "persona",
        "matriculas",
        "matriculas.periodo",
        "matriculas.periodo.seccion",
        "matriculas.periodo.grado"
      })
  List<Estudiante> findAllByActiveTrueOrderByIdAsc();

  boolean existsByPersonaId(Integer personaId);

  boolean existsByPersonaDni(String dni);

  java.util.Optional<Estudiante> findByPersonaDni(String dni);

  @EntityGraph(attributePaths = "persona")
  @org.springframework.data.jpa.repository.Query(
      "select e from Estudiante e where e.active = true "
          + "and (:nombre is null or lower(concat(e.persona.nombre, ' ', e.persona.apellidoPaterno, ' ', e.persona.apellidoMaterno)) like lower(concat('%', :nombre, '%'))) "
          + "and (:dni is null or e.persona.dni like concat('%', :dni, '%')) "
          + "and (:gradoId is null or exists (select m.id from Matricula m where m.estudiante = e and m.active = true and m.periodo.grado.id = :gradoId)) "
          + "and (:seccionId is null or exists (select m.id from Matricula m where m.estudiante = e and m.active = true and m.periodo.seccion.id = :seccionId))")
  Page<Estudiante> buscar(
      @Param("nombre") String nombre,
      @Param("dni") String dni,
      @Param("gradoId") Integer gradoId,
      @Param("seccionId") Integer seccionId,
      Pageable pageable);
}
