package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.CompromisoPago;

public interface CompromisoPagoRepository extends JpaRepository<CompromisoPago, Integer> {
  Optional<CompromisoPago> findByMatriculaId(Integer matriculaId);

  Optional<CompromisoPago> findByMatriculaIdAndActiveTrue(Integer matriculaId);

  @EntityGraph(attributePaths = {"matricula", "matricula.estudiante", "apoderado"})
  List<CompromisoPago> findAllByActiveTrue();

  @Query("select c.id from CompromisoPago c where c.active = true")
  List<Integer> findAllActiveIds();

  @Query(
      "select c from CompromisoPago c where c.active = true "
          + "and c.matricula.estudiante.id = :estudianteId "
          + "and c.matricula.estado <> pe.edu.pucp.colegio.common.model.EstadoMatricula.RETIRADA "
          + "order by c.mesInicio desc")
  List<CompromisoPago> listarVigentesDeEstudiante(@Param("estudianteId") Integer estudianteId);
}
