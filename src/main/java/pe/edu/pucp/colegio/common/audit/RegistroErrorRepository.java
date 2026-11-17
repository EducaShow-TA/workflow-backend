package pe.edu.pucp.colegio.common.audit;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroErrorRepository
    extends JpaRepository<RegistroError, Integer>, JpaSpecificationExecutor<RegistroError> {

  @Modifying
  @Query("delete from RegistroError r where r.fechaHora < :limite")
  int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
