package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.DiaNoLectivo;

public interface DiaNoLectivoRepository extends JpaRepository<DiaNoLectivo, Integer> {

  List<DiaNoLectivo> findAllByActiveTrueAndFechaBetweenOrderByFecha(
      LocalDate desde, LocalDate hasta);

  Optional<DiaNoLectivo> findByFecha(LocalDate fecha);
}
