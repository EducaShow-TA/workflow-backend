package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Calificacion;

public interface CalificacionRepository extends JpaRepository<Calificacion, Integer> {
  boolean existsByCompetenciaCodigo(String competenciaCodigo);

  Optional<Calificacion> findByPeriodoIdAndMatriculaIdAndCompetenciaCodigoAndActiveTrue(
      Integer periodoId, Integer matriculaId, String competenciaCodigo);

  Optional<Calificacion> findByPeriodoIdAndMatriculaIdAndCompetenciaCodigo(
      Integer periodoId, Integer matriculaId, String competenciaCodigo);

  Optional<Calificacion> findByPeriodoIdAndMatriculaIdAndCompetenciaCodigoAndBimestre(
      Integer periodoId, Integer matriculaId, String competenciaCodigo, Integer bimestre);

  Optional<Calificacion>
      findByPeriodoIdAndMatriculaIdAndCompetenciaCodigoAndBimestreAndConsolidadaFalse(
          Integer periodoId, Integer matriculaId, String competenciaCodigo, Integer bimestre);

  Optional<Calificacion>
      findByPeriodoIdAndMatriculaIdAndCompetenciaCodigoAndBimestreIsNullAndConsolidadaFalse(
          Integer periodoId, Integer matriculaId, String competenciaCodigo);

  @EntityGraph(
      attributePaths = {
        "matricula",
        "matricula.estudiante",
        "matricula.estudiante.persona",
        "competencia"
      })
  List<Calificacion>
      findAllByPeriodoIdAndCompetenciaCodigoAndBimestreAndActiveTrueOrderByMatriculaId(
          Integer periodoId, String competenciaCodigo, Integer bimestre);

  @EntityGraph(
      attributePaths = {
        "matricula",
        "matricula.estudiante",
        "matricula.estudiante.persona",
        "competencia"
      })
  List<Calificacion> findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrueOrderByMatriculaId(
      Integer periodoId, String competenciaCodigo);

  List<Calificacion> findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrue(
      Integer periodoId, String competenciaCodigo);

  @EntityGraph(attributePaths = {"matricula"})
  List<Calificacion> findAllByPeriodoIdAndCompetenciaCodigoInAndActiveTrue(
      Integer periodoId, List<String> competenciaCodigos);

  @EntityGraph(attributePaths = {"matricula", "competencia"})
  List<Calificacion> findAllByPeriodoIdAndCompetenciaCodigo(
      Integer periodoId, String competenciaCodigo);

  @EntityGraph(attributePaths = {"competencia"})
  List<Calificacion> findAllByPeriodoIdAndMatriculaIdAndActiveTrue(
      Integer periodoId, Integer matriculaId);

  Optional<Calificacion> findByIdAndActiveTrue(Integer id);
}
