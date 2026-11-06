package pe.edu.pucp.colegio.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaCalificacionRepository
    extends JpaRepository<AuditoriaCalificacion, Integer> {

  @Query(
      "select a from AuditoriaCalificacion a "
          + "where (:periodoId is null or a.periodoId = :periodoId) "
          + "and (:matriculaId is null or a.matriculaId = :matriculaId) "
          + "and (:estudianteId is null or a.estudianteId = :estudianteId) "
          + "and (:competenciaCodigo is null or a.competenciaCodigo = :competenciaCodigo) "
          + "and (:tipoCalificacion is null or a.tipoCalificacion = :tipoCalificacion)")
  Page<AuditoriaCalificacion> buscar(
      @Param("periodoId") Integer periodoId,
      @Param("matriculaId") Integer matriculaId,
      @Param("estudianteId") Integer estudianteId,
      @Param("competenciaCodigo") String competenciaCodigo,
      @Param("tipoCalificacion") String tipoCalificacion,
      Pageable pageable);
}
