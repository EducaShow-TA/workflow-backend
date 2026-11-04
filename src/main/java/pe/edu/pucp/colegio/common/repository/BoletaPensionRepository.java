package pe.edu.pucp.colegio.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.pucp.colegio.common.dto.EstudianteConBoletasResponse;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;

public interface BoletaPensionRepository
    extends JpaRepository<BoletaPension, Integer>,
        JpaSpecificationExecutor<BoletaPension>,
        BoletaPensionAgregados {
  @Override
  @EntityGraph(
      attributePaths = {
        "compromiso.apoderado.persona",
        "compromiso.matricula.estudiante.persona",
        "compromiso.matricula.periodo.grado",
        "compromiso.matricula.periodo.seccion"
      })
  Page<BoletaPension> findAll(Specification<BoletaPension> filtro, Pageable pagina);

  @Override
  @EntityGraph(
      attributePaths = {
        "compromiso.apoderado.persona",
        "compromiso.matricula.estudiante.persona",
        "compromiso.matricula.periodo.grado",
        "compromiso.matricula.periodo.seccion"
      })
  List<BoletaPension> findAll(Specification<BoletaPension> filtro, Sort orden);

  @Query(
      "select distinct new pe.edu.pucp.colegio.common.dto.EstudianteConBoletasResponse("
          + "e.id, concat(p.nombre, ' ', p.apellidoPaterno, ' ', p.apellidoMaterno)) "
          + "from BoletaPension b join b.compromiso c join c.matricula m join m.estudiante e join e.persona p "
          + "where b.active = true order by 2, 1")
  List<EstudianteConBoletasResponse> listarEstudiantesConBoletas();

  boolean existsByCompromisoIdAndMesCobroAndActiveTrue(Integer compromisoId, LocalDate mesCobro);

  @Query(
      "select count(b) > 0 from BoletaPension b "
          + "where b.active = true and b.mesCobro = :mes "
          + "and lower(b.compromiso.concepto) = lower(:concepto) "
          + "and b.compromiso.matricula.estudiante.id = :estudianteId")
  boolean existsByEstudianteConceptoMes(
      @Param("estudianteId") Integer estudianteId,
      @Param("concepto") String concepto,
      @Param("mes") LocalDate mes);

  Optional<BoletaPension> findByIdAndActiveTrue(Integer id);

  @EntityGraph(
      attributePaths = {
        "compromiso",
        "compromiso.apoderado",
        "compromiso.apoderado.persona",
        "compromiso.matricula",
        "compromiso.matricula.estudiante",
        "compromiso.matricula.estudiante.persona",
        "compromiso.matricula.periodo",
        "compromiso.matricula.periodo.grado",
        "compromiso.matricula.periodo.seccion"
      })
  List<BoletaPension> findAllByActiveTrue();

  @EntityGraph(attributePaths = {"compromiso", "compromiso.matricula"})
  List<BoletaPension> findAllByActiveTrueAndEstadoPagoInAndFechaVencimientoBefore(
      List<EstadoBoleta> estados, LocalDate fecha);

  List<BoletaPension> findAllByCompromisoIdAndActiveTrueOrderByMesCobroAsc(Integer compromisoId);
}
