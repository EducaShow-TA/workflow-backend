package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.PlantillaHorario;
import pe.edu.pucp.colegio.common.model.TurnoEscolar;

public interface PlantillaHorarioRepository extends JpaRepository<PlantillaHorario, Integer> {

  @EntityGraph(attributePaths = "bloques")
  List<PlantillaHorario> findAllByOrderByTurnoAscNombreAsc();

  @EntityGraph(attributePaths = "bloques")
  Optional<PlantillaHorario> findWithBloquesById(Integer id);

  boolean existsByTurnoAndNombreIgnoreCaseAndIdNot(TurnoEscolar turno, String nombre, Integer id);

  boolean existsByTurnoAndNombreIgnoreCase(TurnoEscolar turno, String nombre);
}
