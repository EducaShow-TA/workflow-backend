package pe.edu.pucp.colegio.common.repository;

import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.pucp.colegio.common.model.BoletaPension;

public interface BoletaPensionAgregados {
  BigDecimal sumarPendiente(Specification<BoletaPension> filtro);

  long contarFamiliasMorosas(Specification<BoletaPension> filtro);
}
