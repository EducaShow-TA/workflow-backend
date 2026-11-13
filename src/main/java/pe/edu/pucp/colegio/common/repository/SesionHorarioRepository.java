package pe.edu.pucp.colegio.common.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.SesionHorario;

public interface SesionHorarioRepository extends JpaRepository<SesionHorario, Integer> {

  @Query(
      "select case when count(s) > 0 then true else false end from SesionHorario s "
          + "where s.bloque.plantilla.id = :plantillaId")
  boolean existsByPlantillaId(@Param("plantillaId") Integer plantillaId);

  @Query(
      "select s from SesionHorario s "
          + "join fetch s.horario h "
          + "join fetch h.seccion sec "
          + "join fetch sec.grado "
          + "join fetch s.bloque b "
          + "join fetch s.area "
          + "join fetch s.docente "
          + "join fetch s.aula "
          + "where s.docente.id = :cuentaId and h.anio = :anio "
          + "and s.vigenteDesde <= :hasta and (s.vigenteHasta is null or s.vigenteHasta >= :desde) "
          + "order by s.dia, b.horaInicio")
  List<SesionHorario> listarHorarioDocente(
      @Param("cuentaId") Integer cuentaId,
      @Param("anio") Integer anio,
      @Param("desde") java.time.LocalDate desde,
      @Param("hasta") java.time.LocalDate hasta);

  @EntityGraph(attributePaths = {"bloque", "area", "docente", "docente.persona", "aula"})
  List<SesionHorario> findAllByHorarioId(Integer horarioId);

  @Query(
      "select distinct s from SesionHorario s join fetch s.area a left join fetch a.competencias "
          + "where s.docente.persona.id = :personaId and s.horario.seccion.id = :seccionId "
          + "and s.horario.active = true and s.vigenteDesde <= :hasta "
          + "and (s.vigenteHasta is null or s.vigenteHasta >= :desde)")
  List<SesionHorario> buscarAsignacionesDocente(
      @Param("personaId") Integer personaId,
      @Param("seccionId") Integer seccionId,
      @Param("desde") java.time.LocalDate desde,
      @Param("hasta") java.time.LocalDate hasta);

  Optional<SesionHorario> findByIdAndHorarioId(Integer id, Integer horarioId);

  boolean existsByAulaId(Integer aulaId);

  @Query(
      "select case when count(s) > 0 then true else false end from SesionHorario s"
          + " where s.horario.id = :horarioId and s.bloque.id = :bloqueId and s.dia = :dia"
          + " and s.vigenteDesde <= :fecha and (s.vigenteHasta is null or s.vigenteHasta >= :fecha)"
          + " and (:excluirId is null or s.id <> :excluirId)")
  boolean existeCeldaVigente(
      @Param("horarioId") Integer horarioId,
      @Param("bloqueId") Integer bloqueId,
      @Param("dia") DayOfWeek dia,
      @Param("fecha") java.time.LocalDate fecha,
      @Param("excluirId") Integer excluirId);

  /** Horarios (sección y año) que tienen al menos una clase en el aula. */
  @Query("select distinct s.horario from SesionHorario s where s.aula.id = :aulaId")
  List<HorarioSeccion> findHorariosConAula(@Param("aulaId") Integer aulaId);

  @Query(
      """
            select s from SesionHorario s
              join fetch s.horario h
              join fetch h.seccion sec
              join fetch sec.grado
              join fetch s.bloque b
            where s.docente.id = :docenteId
              and s.dia = :dia
              and h.anio = :anio
              and s.vigenteDesde <= :fecha
              and (s.vigenteHasta is null or s.vigenteHasta >= :fecha)
              and s.id <> :excluirId
              and b.horaInicio < :fin
              and b.horaFin > :inicio
            order by b.horaInicio
            """)
  List<SesionHorario> buscarCrucesDocente(
      @Param("docenteId") Integer docenteId,
      @Param("dia") DayOfWeek dia,
      @Param("anio") Integer anio,
      @Param("fecha") java.time.LocalDate fecha,
      @Param("inicio") LocalTime inicio,
      @Param("fin") LocalTime fin,
      @Param("excluirId") Integer excluirId);

  @Query(
      """
            select s from SesionHorario s
              join fetch s.horario h
              join fetch h.seccion sec
              join fetch sec.grado
              join fetch s.bloque b
            where s.aula.id = :aulaId
              and s.dia = :dia
              and h.anio = :anio
              and s.vigenteDesde <= :fecha
              and (s.vigenteHasta is null or s.vigenteHasta >= :fecha)
              and s.id <> :excluirId
              and b.horaInicio < :fin
              and b.horaFin > :inicio
            order by b.horaInicio
            """)
  List<SesionHorario> buscarCrucesAula(
      @Param("aulaId") Integer aulaId,
      @Param("dia") DayOfWeek dia,
      @Param("anio") Integer anio,
      @Param("fecha") java.time.LocalDate fecha,
      @Param("inicio") LocalTime inicio,
      @Param("fin") LocalTime fin,
      @Param("excluirId") Integer excluirId);
}
