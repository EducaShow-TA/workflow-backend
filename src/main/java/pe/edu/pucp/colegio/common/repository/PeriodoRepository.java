package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.Periodo;

public interface PeriodoRepository extends JpaRepository<Periodo, Integer> {

  @Query(
      "select p from Periodo p where p.active = true and p.seccion.id = :seccionId"
          + " and p.fechaInicio <= :fechaFin and p.fechaFin >= :fechaInicio"
          + " order by p.fechaInicio")
  List<Periodo> buscarCruces(
      @Param("seccionId") Integer seccionId,
      @Param("fechaInicio") LocalDate fechaInicio,
      @Param("fechaFin") LocalDate fechaFin);

  @Query(
      "select p from Periodo p where p.active = true"
          + " and p.fechaInicio between :desde and :hasta"
          + " and (:gradoId is null or p.grado.id = :gradoId)"
          + " and (:seccionId is null or p.seccion.id = :seccionId)"
          + " order by p.grado.numero, p.seccion.codigo, p.fechaInicio")
  List<Periodo> buscar(
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta,
      @Param("gradoId") Integer gradoId,
      @Param("seccionId") Integer seccionId);

  Optional<Periodo> findFirstBySeccionIdAndActiveTrueOrderByFechaInicioDesc(Integer seccionId);

  boolean existsBySeccionIdAndActiveTrueAndFechaInicioBetween(
      Integer seccionId, LocalDate desde, LocalDate hasta);

  List<Periodo> findAllByActiveTrueAndFechaFinGreaterThanEqual(LocalDate fecha);
}
