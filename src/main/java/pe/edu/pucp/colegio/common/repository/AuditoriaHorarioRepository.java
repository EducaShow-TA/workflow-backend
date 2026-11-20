package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.AuditoriaHorario;

public interface AuditoriaHorarioRepository extends JpaRepository<AuditoriaHorario, Integer> {
  List<AuditoriaHorario> findAllByFechaHoraBetweenOrderByFechaHoraDesc(
      LocalDateTime desde, LocalDateTime hasta);

  List<AuditoriaHorario> findAllByHorarioIdAndFechaHoraBetweenOrderByFechaHoraDesc(
      Integer horarioId, LocalDateTime desde, LocalDateTime hasta);
}
