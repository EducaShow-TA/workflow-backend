package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;

public interface BoletaPensionRepository extends JpaRepository<BoletaPension, Integer> {
  boolean existsByCompromisoIdAndMesCobroAndActiveTrue(Integer compromisoId, LocalDate mesCobro);

  @Query("select count(b) > 0 from BoletaPension b "
      + "where b.active = true and b.mesCobro = :mes "
      + "and lower(b.compromiso.concepto) = lower(:concepto) "
      + "and b.compromiso.matricula.estudiante.id = :estudianteId")
  boolean existsByEstudianteConceptoMes(
      @Param("estudianteId") Integer estudianteId,
      @Param("concepto") String concepto,
      @Param("mes") LocalDate mes);

  Optional<BoletaPension> findByIdAndActiveTrue(Integer id);

  @EntityGraph(attributePaths = {"compromiso", "compromiso.apoderado", "compromiso.apoderado.persona",
      "compromiso.matricula", "compromiso.matricula.estudiante", "compromiso.matricula.estudiante.persona",
      "compromiso.matricula.periodo", "compromiso.matricula.periodo.grado", "compromiso.matricula.periodo.seccion"})
  List<BoletaPension> findAllByActiveTrue();

  @EntityGraph(attributePaths = {"compromiso", "compromiso.matricula"})
  List<BoletaPension> findAllByActiveTrueAndEstadoPagoInAndFechaVencimientoBefore(
      List<EstadoBoleta> estados, LocalDate fecha);

  List<BoletaPension> findAllByCompromisoIdAndActiveTrueOrderByMesCobroAsc(Integer compromisoId);
}
