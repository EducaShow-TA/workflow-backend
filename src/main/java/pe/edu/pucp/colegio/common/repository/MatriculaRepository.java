package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
  Optional<Matricula> findByEstudianteIdAndPeriodoIdAndEstado(
      Integer estudianteId, Integer periodoId, EstadoMatricula estado);

  @EntityGraph(attributePaths = {"estudiante", "estudiante.persona"})
  List<Matricula> findAllByIdIn(List<Integer> ids);

  @org.springframework.data.jpa.repository.EntityGraph(
      attributePaths = {"periodo", "periodo.grado", "periodo.seccion"})
  List<Matricula> findAllByEstudianteIdIn(List<Integer> estudianteIds);

  Optional<Matricula> findByPeriodoIdAndEstudianteId(Integer periodoId, Integer estudianteId);

  @EntityGraph(
      attributePaths = {
        "estudiante",
        "estudiante.persona",
        "periodo",
        "periodo.grado",
        "periodo.seccion"
      })
  Optional<Matricula> findByIdAndActiveTrue(Integer id);

  @EntityGraph(
      attributePaths = {
        "estudiante",
        "estudiante.persona",
        "periodo",
        "periodo.grado",
        "periodo.seccion"
      })
  @Query(
      "select m from Matricula m where m.estudiante.id = :estudianteId "
          + "and m.active = true and m.estado = :estado and m.periodo.active = true "
          + "and m.periodo.fechaInicio between :desde and :hasta "
          + "and m.periodo.fechaInicio <= :fechaCorte "
          + "and (m.periodo.id = :periodoId or m.periodo.actasCerradas = true) "
          + "order by m.periodo.fechaInicio, m.periodo.id")
  List<Matricula> buscarParaLibreta(
      @Param("estudianteId") Integer estudianteId,
      @Param("periodoId") Integer periodoId,
      @Param("estado") EstadoMatricula estado,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      @Param("fechaCorte") LocalDate fechaCorte);

  @org.springframework.data.jpa.repository.EntityGraph(
      attributePaths = {"estudiante", "estudiante.persona"})
  @Query(
      "select m from Matricula m where m.periodo.id = :periodoId and m.active = true "
          + "and m.estudiante.active = true and m.estado in :estados")
  List<Matricula> findAllByPeriodoIdAndActiveTrueAndEstadoIn(
      @Param("periodoId") Integer periodoId,
      @Param("estados") List<pe.edu.pucp.colegio.common.model.EstadoMatricula> estados);

  @Query(
      "select count(distinct m.estudiante.id) from Matricula m "
          + "where m.periodo.seccion.id = :seccionId and m.active = true "
          + "and m.estudiante.active = true "
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

  /** Matrículas registradas sin confirmar: reservan vacante, pero aún no son alumnos del aula. */
  @Query(
      "select count(distinct m.estudiante.id) from Matricula m "
          + "where m.periodo.seccion.id = :seccionId and m.active = true "
          + "and m.estudiante.active = true "
          + "and m.periodo.fechaInicio between :desde and :hasta "
          + "and m.estado = pe.edu.pucp.colegio.common.model.EstadoMatricula.EN_REGISTRO")
  long countPendientesPorSeccionEntre(
      @Param("seccionId") Integer seccionId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);

  default long countPendientesPorSeccionEnAnhio(Integer seccionId, int anhio) {
    return countPendientesPorSeccionEntre(
        seccionId, LocalDate.of(anhio, 1, 1), LocalDate.of(anhio, 12, 31));
  }

  @Query(
      "select case when count(m) > 0 then true else false end from Matricula m "
          + "where m.active = true and m.periodo.fechaInicio between :desde and :hasta")
  boolean existsByActiveTrueAndPeriodoFechaInicioBetween(
      @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

  /**
   * Matrículas vigentes (no retiradas) del estudiante en otros periodos del mismo rango de años.
   */
  @org.springframework.data.jpa.repository.EntityGraph(
      attributePaths = {"periodo", "periodo.grado", "periodo.seccion"})
  @Query(
      "select m from Matricula m where m.estudiante.id = :estudianteId and m.active = true "
          + "and m.estado <> pe.edu.pucp.colegio.common.model.EstadoMatricula.RETIRADA "
          + "and m.periodo.id <> :periodoId "
          + "and m.periodo.fechaInicio between :desde and :hasta")
  List<Matricula> buscarOtrasMatriculasEntre(
      @Param("estudianteId") Integer estudianteId,
      @Param("periodoId") Integer periodoId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);

  /** Alumnos con matrícula confirmada en la sección durante el periodo que incluye la fecha. */
  @org.springframework.data.jpa.repository.EntityGraph(
      attributePaths = {"estudiante", "estudiante.persona"})
  @Query(
      "select m from Matricula m where m.active = true and m.estudiante.active = true "
          + "and m.estado = pe.edu.pucp.colegio.common.model.EstadoMatricula.CONFIRMADA "
          + "and m.periodo.seccion.id = :seccionId "
          + "and m.periodo.fechaInicio <= :fecha and m.periodo.fechaFin >= :fecha")
  List<Matricula> buscarConfirmadasEnSeccion(
      @Param("seccionId") Integer seccionId, @Param("fecha") LocalDate fecha);
}
