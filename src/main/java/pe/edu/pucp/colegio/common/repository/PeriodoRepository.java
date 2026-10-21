package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Periodo;

public interface PeriodoRepository extends JpaRepository<Periodo, Integer> {

  boolean existsByGradoIdAndSeccionIdAndFechaInicioAndFechaFin(
      Integer gradoId, Integer seccionId, LocalDate fechaInicio, LocalDate fechaFin);
}
