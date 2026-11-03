package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.ComprobantePago;

public interface ComprobantePagoRepository extends JpaRepository<ComprobantePago, Integer> {
  boolean existsByBoletaIdAndActiveTrue(Integer boletaId);
  boolean existsByNumeroComprobante(String numeroComprobante);
  List<ComprobantePago> findAllByBoletaIdAndActiveTrueOrderByFechaEmisionDesc(Integer boletaId);
  Optional<ComprobantePago> findByNumeroComprobante(String numeroComprobante);
}
