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

    @EntityGraph(attributePaths = {"bloque", "area", "docente", "docente.persona", "aula"})
    List<SesionHorario> findAllByHorarioId(Integer horarioId);

    Optional<SesionHorario> findByIdAndHorarioId(Integer id, Integer horarioId);

    boolean existsByAulaId(Integer aulaId);

    boolean existsByHorarioIdAndBloqueIdAndDia(Integer horarioId, Integer bloqueId, DayOfWeek dia);

    boolean existsByHorarioIdAndBloqueIdAndDiaAndIdNot(
            Integer horarioId, Integer bloqueId, DayOfWeek dia, Integer id);

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
              and s.id <> :excluirId
              and b.horaInicio < :fin
              and b.horaFin > :inicio
            order by b.horaInicio
            """)
    List<SesionHorario> buscarCrucesDocente(
            @Param("docenteId") Integer docenteId,
            @Param("dia") DayOfWeek dia,
            @Param("anio") Integer anio,
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
              and s.id <> :excluirId
              and b.horaInicio < :fin
              and b.horaFin > :inicio
            order by b.horaInicio
            """)
    List<SesionHorario> buscarCrucesAula(
            @Param("aulaId") Integer aulaId,
            @Param("dia") DayOfWeek dia,
            @Param("anio") Integer anio,
            @Param("inicio") LocalTime inicio,
            @Param("fin") LocalTime fin,
            @Param("excluirId") Integer excluirId);
}