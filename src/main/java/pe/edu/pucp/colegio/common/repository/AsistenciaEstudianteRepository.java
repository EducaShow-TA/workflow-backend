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

  String ESTADO = "pe.edu.pucp.colegio.common.model.EstadoAsistencia.";

  boolean existsBySesionClaseIdAndActiveTrue(Integer sesionClaseId);

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

  /**
   * Consolida por alumno y sección los estados marcados en el rango (US-2.6). Excluye pendientes,
   * registros y sesiones inactivos y sesiones canceladas. Con {@code personaIdDocente} aplica la
   * regla del historial: sesiones que dicta o de secciones donde tiene clase vigente ese día.
   */
  @Query(
      "select e.id as estudianteId, p.nombre as nombre, p.apellidoPaterno as apellidoPaterno,"
          + " p.apellidoMaterno as apellidoMaterno, g.id as gradoId, g.numero as gradoNumero,"
          + " g.nivelEducativo as nivelEducativo, sec.id as seccionId, sec.codigo as seccionCodigo,"
          + " sum(case when a.estadoAsistencia = "
          + ESTADO
          + "PRESENTE then 1 else 0 end) as presentes,"
          + " sum(case when a.estadoAsistencia = "
          + ESTADO
          + "TARDANZA then 1 else 0 end) as tardanzas,"
          + " sum(case when a.estadoAsistencia = "
          + ESTADO
          + "AUSENTE then 1 else 0 end) as inasistencias,"
          + " sum(case when a.estadoAsistencia = "
          + ESTADO
          + "JUSTIFICADA then 1 else 0 end) as justificadas"
          + " from AsistenciaEstudiante a join a.estudiante e join e.persona p"
          + " join a.sesionClase s join s.seccion sec join sec.grado g"
          + " where a.active = true and s.active = true and s.cancelada = false"
          + " and s.fecha between :desde and :hasta"
          + " and a.estadoAsistencia <> "
          + ESTADO
          + "PENDIENTE_MARCAJE"
          + " and (:gradoId is null or g.id = :gradoId)"
          + " and (:seccionId is null or sec.id = :seccionId)"
          + " and (:personaId is null or s.docente.persona.id = :personaId or exists ("
          + " select sh.id from SesionHorario sh where sh.docente.persona.id = :personaId"
          + " and sh.horario.seccion = sec and sh.horario.active = true"
          + " and sh.vigenteDesde <= s.fecha"
          + " and (sh.vigenteHasta is null or sh.vigenteHasta >= s.fecha)))"
          + " group by e.id, p.nombre, p.apellidoPaterno, p.apellidoMaterno, g.id, g.numero,"
          + " g.nivelEducativo, sec.id, sec.codigo")
  List<ConsolidadoAsistenciaFila> consolidarPorAlumno(
      @Param("desde") java.time.LocalDate desde,
      @Param("hasta") java.time.LocalDate hasta,
      @Param("gradoId") Integer gradoId,
      @Param("seccionId") Integer seccionId,
      @Param("personaId") Integer personaIdDocente);
}
