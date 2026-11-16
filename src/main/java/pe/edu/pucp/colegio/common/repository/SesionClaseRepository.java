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
          + "and s.cancelada = false "
          + "and (:fecha is null or s.fecha = :fecha) "
          + "and (:seccionId is null or s.seccion.id = :seccionId) "
          + "order by s.fecha desc, s.horaInicio desc")
  List<SesionClase> buscar(@Param("fecha") LocalDate fecha, @Param("seccionId") Integer seccionId);

  /** Pares (sección, área) que dicta el docente: cada par es una de sus clases. */
  @Query(
      "select distinct sh.horario.seccion.id, sh.area.codigo from SesionHorario sh"
          + " where sh.docente.persona.id = :personaId and sh.horario.active = true"
          + " and sh.vigenteDesde <= :hasta and (sh.vigenteHasta is null or sh.vigenteHasta >= :desde)")
  List<Object[]> buscarClasesDelDocente(
      @Param("personaId") Integer personaId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);

  boolean existsByDocenteIdAndSeccionIdAndActiveTrue(Integer docenteId, Integer seccionId);

  @Query(
      "select case when count(sh) > 0 then true else false end from SesionHorario sh"
          + " where sh.docente.persona.id = :personaId"
          + " and sh.horario.seccion.id = :seccionId and sh.horario.active = true"
          + " and sh.vigenteDesde <= :hasta and (sh.vigenteHasta is null or sh.vigenteHasta >= :desde)")
  boolean docenteTieneClaseEnSeccion(
      @Param("personaId") Integer personaId,
      @Param("seccionId") Integer seccionId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);

  @Query(
      "select case when count(sh) > 0 then true else false end from SesionHorario sh"
          + " where sh.docente.persona.id = :personaId"
          + " and sh.horario.seccion.id = :seccionId and sh.area.codigo = :areaCodigo"
          + " and sh.horario.active = true"
          + " and sh.vigenteDesde <= :hasta and (sh.vigenteHasta is null or sh.vigenteHasta >= :desde)")
  boolean docenteTieneAreaEnSeccion(
      @Param("personaId") Integer personaId,
      @Param("seccionId") Integer seccionId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      @Param("areaCodigo") String areaCodigo);

  boolean existsByDocenteIdAndSeccionIdAndAreaCodigoAndActiveTrue(
      Integer docenteId, Integer seccionId, String areaCodigo);

  @EntityGraph(attributePaths = {"area", "area.competencias"})
  List<SesionClase> findAllByDocenteIdAndSeccionIdAndActiveTrue(
      Integer docenteId, Integer seccionId);

  @EntityGraph(attributePaths = {"area", "area.competencias"})
  List<SesionClase> findAllByDocenteIdAndSeccionIdAndFechaBetweenAndActiveTrue(
      Integer docenteId, Integer seccionId, LocalDate desde, LocalDate hasta);

  @EntityGraph(attributePaths = {"seccion", "seccion.grado", "area", "docente", "docente.persona"})
  List<SesionClase> findAllByDocenteIdAndFechaAndActiveTrueAndCanceladaFalseOrderByHoraInicioAsc(
      Integer docenteId, LocalDate fecha);

  @EntityGraph(attributePaths = {"seccion", "area", "docente", "docente.persona"})
  List<SesionClase> findAllBySesionHorarioIdAndFechaAndActiveTrue(
      Integer sesionHorarioId, LocalDate fecha);

  boolean existsBySesionHorarioIdAndFecha(Integer sesionHorarioId, LocalDate fecha);

  boolean existsBySesionHorarioId(Integer sesionHorarioId);

  List<SesionClase> findAllByFechaAndSesionHorarioIsNotNullAndActiveTrue(LocalDate fecha);

  List<SesionClase> findAllBySesionHorarioIdAndFechaAfterAndActiveTrue(
      Integer sesionHorarioId, LocalDate fecha);

  boolean existsBySeccionIdAndAreaCodigoAndFechaAndHoraInicioAndActiveTrue(
      Integer seccionId, String areaCodigo, LocalDate fecha, java.time.LocalTime horaInicio);

  Optional<SesionClase> findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(
      Integer docenteId, LocalDate fecha);
}
