package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.Tarea;

public interface TareaRepository extends JpaRepository<Tarea, Integer> {
  @Query("select t from Tarea t join fetch t.periodo join fetch t.competencia where t.id = :id and t.active = true")
  java.util.Optional<Tarea> buscarActiva(@Param("id") Integer id);

  @Query("select t from Tarea t join fetch t.periodo join fetch t.competencia where t.periodo.id = :periodoId and t.active = true order by t.fechaLimite")
  List<Tarea> buscarPorPeriodo(@Param("periodoId") Integer periodoId);
}
