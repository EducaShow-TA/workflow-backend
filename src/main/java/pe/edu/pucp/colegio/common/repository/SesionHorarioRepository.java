package pe.edu.pucp.colegio.common.repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
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
          + "where s.docente.id = :docenteId and h.anio = :anio "
          + "and s.vigenteDesde <= :hasta and (s.vigenteHasta is null or s.vigenteHasta >= :desde) "
          + "and s.vigenteDesde <= :fecha and (s.vigenteHasta is null or s.vigenteHasta >= :fecha) "
          + "order by s.dia, b.horaInicio")
  List<SesionHorario> listarHorarioDocente(
      @Param("docenteId") Integer docenteId,
      @Param("anio") Integer anio,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      @Param("fecha") LocalDate fecha);

  @EntityGraph(attributePaths = "docente.persona")
  @Query(
      "select distinct s from SesionHorario s "
          + "join fetch s.horario h "
          + "join fetch h.seccion sec "
          + "join fetch sec.grado "
          + "join fetch s.bloque b "
          + "join fetch s.area "
          + "join fetch s.docente "
          + "join fetch s.aula "
          + "where h.anio = :anio "
          + "and s.vigenteDesde <= :hasta "
          + "and (s.vigenteHasta is null or s.vigenteHasta >= :desde) "
          + "and s.vigenteDesde <= :fecha and (s.vigenteHasta is null or s.vigenteHasta >= :fecha) "
          + "and exists (select m.id from Matricula m "
          + "where m.estudiante.persona.id = :personaId "
          + "and m.active = true and m.estado = :estado "
          + "and m.periodo.seccion = h.seccion "
          + "and m.periodo.fechaInicio <= :hasta and m.periodo.fechaFin >= :desde "
          + "and m.periodo.fechaInicio <= coalesce(s.vigenteHasta, :hasta) "
          + "and m.periodo.fechaFin >= s.vigenteDesde) "
          + "order by s.dia, b.horaInicio")
  List<SesionHorario> listarHorarioEstudiante(
      @Param("personaId") Integer personaId,
      @Param("anio") Integer anio,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      @Param("fecha") LocalDate fecha,
      @Param("estado") EstadoMatricula estado);

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
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);

  @Query(
      "select distinct s from SesionHorario s join fetch s.area a left join fetch a.competencias "
          + "join fetch s.docente d join fetch d.persona "
          + "where s.horario.seccion.id = :seccionId and s.horario.anio = :anio "
          + "and s.horario.active = true")
  List<SesionHorario> listarAsignacionesSeccion(
      @Param("seccionId") Integer seccionId, @Param("anio") Integer anio);

  Optional<SesionHorario> findByIdAndHorarioId(Integer id, Integer horarioId);

  boolean existsByHorarioIdAndDocenteId(Integer horarioId, Integer docenteId);

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
      @Param("fecha") LocalDate fecha,
      @Param("excluirId") Integer excluirId);

  /** Horarios (sección y año) que tienen al menos una clase en el aula. */
  @Query("select distinct s.horario from SesionHorario s where s.aula.id = :aulaId")
  List<HorarioSeccion> findHorariosConAula(@Param("aulaId") Integer aulaId);

  @Query(
      "select s from SesionHorario s join fetch s.aula "
          + "where s.horario.seccion.id = :seccionId and s.horario.active = true "
          + "and s.horario.anio >= :anioActual "
          + "and (s.vigenteHasta is null or s.vigenteHasta >= :hoy)")
  List<SesionHorario> findVigentesBySeccionId(
      @Param("seccionId") Integer seccionId,
      @Param("hoy") LocalDate hoy,
      @Param("anioActual") Integer anioActual);

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
      @Param("fecha") LocalDate fecha,
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
      @Param("fecha") LocalDate fecha,
      @Param("inicio") LocalTime inicio,
      @Param("fin") LocalTime fin,
      @Param("excluirId") Integer excluirId);
}
