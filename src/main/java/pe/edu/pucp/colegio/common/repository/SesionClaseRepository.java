package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.SesionClase;

public interface SesionClaseRepository extends JpaRepository<SesionClase, Integer> {

  @EntityGraph(attributePaths = {"seccion", "seccion.grado", "area", "docente", "docente.persona"})
  Optional<SesionClase> findByIdAndActiveTrue(Integer id);

  @EntityGraph(attributePaths = {"seccion", "seccion.grado", "area", "docente", "docente.persona"})
  @Query(
      "select s from SesionClase s where s.active = true "
          + "and (:fecha is null or s.fecha = :fecha) "
          + "and (:seccionId is null or s.seccion.id = :seccionId) "
          + "order by s.fecha desc, s.horaInicio desc")
  List<SesionClase> buscar(@Param("fecha") LocalDate fecha, @Param("seccionId") Integer seccionId);

  /** Pares (sección, área) que dicta el docente: cada par es una de sus clases. */
  @Query(
      "select distinct s.seccion.id, s.area.codigo from SesionClase s"
          + " where s.active = true and s.docente.id = :docenteId")
  List<Object[]> buscarClasesDelDocente(@Param("docenteId") Integer docenteId);

  boolean existsByDocenteIdAndSeccionIdAndActiveTrue(Integer docenteId, Integer seccionId);

  boolean existsByDocenteIdAndSeccionIdAndAreaCodigoAndActiveTrue(
      Integer docenteId, Integer seccionId, String areaCodigo);

  @EntityGraph(attributePaths = {"area", "area.competencias"})
  List<SesionClase> findAllByDocenteIdAndSeccionIdAndActiveTrue(
      Integer docenteId, Integer seccionId);

  @EntityGraph(attributePaths = {"area", "area.competencias"})
  List<SesionClase> findAllByDocenteIdAndSeccionIdAndFechaBetweenAndActiveTrue(
      Integer docenteId, Integer seccionId, LocalDate desde, LocalDate hasta);

  @EntityGraph(attributePaths = {"seccion", "seccion.grado", "area", "docente", "docente.persona"})
  List<SesionClase> findAllByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(
      Integer docenteId, LocalDate fecha);

  boolean existsBySeccionIdAndAreaCodigoAndFechaAndHoraInicioAndActiveTrue(
      Integer seccionId, String areaCodigo, LocalDate fecha, java.time.LocalTime horaInicio);

  Optional<SesionClase> findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(
      Integer docenteId, LocalDate fecha);
}
