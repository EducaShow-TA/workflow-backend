package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

  long countByPeriodoIdAndActiveTrueAndEstadoNot(Integer periodoId, EstadoMatricula estado);

  Optional<Matricula> findByPeriodoIdAndEstudianteId(Integer periodoId, Integer estudianteId);

  @Query(
      "select count(distinct m.estudiante.id) from Matricula m "
          + "where m.periodo.seccion.id = :seccionId and m.active = true "
          + "and m.estado in (pe.edu.pucp.colegio.common.model.EstadoMatricula.EN_REGISTRO, "
          + "pe.edu.pucp.colegio.common.model.EstadoMatricula.CONFIRMADA)")
  long countAlumnosActivosPorSeccion(@Param("seccionId") Integer seccionId);
}
