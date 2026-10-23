package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

  @org.springframework.data.jpa.repository.EntityGraph(
      attributePaths = {"periodo", "periodo.grado", "periodo.seccion"})
  List<Matricula> findAllByEstudianteIdIn(List<Integer> estudianteIds);

  Optional<Matricula> findByPeriodoIdAndEstudianteId(Integer periodoId, Integer estudianteId);

  @Query(
      "select count(distinct m.estudiante.id) from Matricula m "
          + "where m.periodo.seccion.id = :seccionId and m.active = true "
          + "and m.periodo.fechaInicio between :desde and :hasta "
          + "and m.estado in (pe.edu.pucp.colegio.common.model.EstadoMatricula.EN_REGISTRO, "
          + "pe.edu.pucp.colegio.common.model.EstadoMatricula.CONFIRMADA)")
  long countAlumnosActivosPorSeccionEntre(
      @Param("seccionId") Integer seccionId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);

  default long countAlumnosActivosPorSeccionEnAnhio(Integer seccionId, int anhio) {
    return countAlumnosActivosPorSeccionEntre(
        seccionId, LocalDate.of(anhio, 1, 1), LocalDate.of(anhio, 12, 31));
  }

  boolean existsByActiveTrueAndFechaMatriculaBetween(LocalDate desde, LocalDate hasta);
}
