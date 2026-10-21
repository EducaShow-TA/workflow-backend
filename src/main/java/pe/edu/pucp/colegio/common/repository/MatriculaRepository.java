package pe.edu.pucp.colegio.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {

  long countByPeriodoIdAndActiveTrueAndEstadoNot(Integer periodoId, EstadoMatricula estado);

  Optional<Matricula> findByPeriodoIdAndEstudianteId(Integer periodoId, Integer estudianteId);
}
