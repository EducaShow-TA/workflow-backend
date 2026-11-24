package pe.edu.pucp.colegio.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.BimestreCalendario;

public interface BimestreCalendarioRepository extends JpaRepository<BimestreCalendario, Integer> {

  List<BimestreCalendario> findAllByAnhioOrderByNumero(Integer anhio);

  boolean existsByAnhioAndNumeroAndCerradoTrue(Integer anhio, Integer numero);
}
