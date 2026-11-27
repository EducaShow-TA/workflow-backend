package pe.edu.pucp.colegio.common.audit;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaCalificacionRepository
    extends JpaRepository<AuditoriaCalificacion, Integer> {

  @Query(
      "select a from AuditoriaCalificacion a "
          + "where (:filtrarAnhio = false or a.periodoId in (select p.id from Periodo p "
          + "where p.fechaInicio >= :desde and p.fechaInicio < :hasta)) "
          + "and (:periodoId is null or a.periodoId = :periodoId) "
          + "and (:matriculaId is null or a.matriculaId = :matriculaId) "
          + "and (:estudianteId is null or a.estudianteId = :estudianteId) "
          + "and (:competenciaCodigo is null or a.competenciaCodigo = :competenciaCodigo) "
          + "and (:tipoCalificacion is null or a.tipoCalificacion = :tipoCalificacion) "
          + "and (:todasLasAcciones = true or a.accion in :acciones)")
  Page<AuditoriaCalificacion> buscar(
      @Param("filtrarAnhio") boolean filtrarAnhio,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      @Param("periodoId") Integer periodoId,
      @Param("matriculaId") Integer matriculaId,
      @Param("estudianteId") Integer estudianteId,
      @Param("competenciaCodigo") String competenciaCodigo,
      @Param("tipoCalificacion") String tipoCalificacion,
      @Param("todasLasAcciones") boolean todasLasAcciones,
      @Param("acciones") Collection<String> acciones,
      Pageable pageable);

  @Query(
      "select a from AuditoriaCalificacion a "
          + "where a.tipoCalificacion = 'FINAL' and a.matriculaId = :matriculaId "
          + "and a.competenciaCodigo = :competenciaCodigo "
          + "and (:bimestre is null or a.bimestre = :bimestre) "
          + "and a.accion in :acciones "
          + "order by a.fechaHora desc, a.id desc")
  List<AuditoriaCalificacion> historialFinal(
      @Param("matriculaId") Integer matriculaId,
      @Param("competenciaCodigo") String competenciaCodigo,
      @Param("bimestre") Integer bimestre,
      @Param("acciones") Collection<String> acciones);

  @Query(
      "select a.matriculaId, a.bimestre, count(a) from AuditoriaCalificacion a "
          + "where a.tipoCalificacion = 'FINAL' and a.accion = :accion "
          + "and a.periodoId = :periodoId and a.competenciaCodigo = :competenciaCodigo "
          + "and (:bimestre is null or a.bimestre = :bimestre) "
          + "group by a.matriculaId, a.bimestre")
  List<Object[]> contarPorMatricula(
      @Param("accion") String accion,
      @Param("periodoId") Integer periodoId,
      @Param("competenciaCodigo") String competenciaCodigo,
      @Param("bimestre") Integer bimestre);
}
