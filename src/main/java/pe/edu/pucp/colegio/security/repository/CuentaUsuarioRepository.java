package pe.edu.pucp.colegio.security.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;

public interface CuentaUsuarioRepository extends JpaRepository<CuentaUsuario, Integer> {

  Optional<CuentaUsuario> findByUsername(String username);

  boolean existsByUsername(String username);
}
