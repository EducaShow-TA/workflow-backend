package pe.edu.pucp.colegio.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.BloqueHorario;

public interface BloqueHorarioRepository extends JpaRepository<BloqueHorario, Integer> {

  void deleteAllByPlantillaId(Integer plantillaId);
}
