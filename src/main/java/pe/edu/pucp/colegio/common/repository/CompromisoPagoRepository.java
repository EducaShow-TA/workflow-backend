package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.CompromisoPago;

public interface CompromisoPagoRepository extends JpaRepository<CompromisoPago, Integer> {
  Optional<CompromisoPago> findByMatriculaIdAndActiveTrue(Integer matriculaId);

  @EntityGraph(attributePaths = {"matricula", "matricula.estudiante", "apoderado"})
  List<CompromisoPago> findAllByActiveTrue();
}
