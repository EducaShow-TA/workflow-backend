package pe.edu.pucp.colegio.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RegistroErrorRepository
    extends JpaRepository<RegistroError, Integer>, JpaSpecificationExecutor<RegistroError> {}
