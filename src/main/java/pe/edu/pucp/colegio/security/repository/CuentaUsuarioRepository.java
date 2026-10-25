package pe.edu.pucp.colegio.security.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;

public interface CuentaUsuarioRepository extends JpaRepository<CuentaUsuario, Integer> {

  Optional<CuentaUsuario> findByUsername(String username);

  Optional<CuentaUsuario> findByUsernameIgnoreCase(String username);

  Optional<CuentaUsuario> findByPersonaDni(String dni);

  List<CuentaUsuario> findAllByPersonaCorreoIgnoreCaseAndActiveTrue(String correo);

  List<CuentaUsuario> findAllByPersonaCorreoIgnoreCase(String correo);

  boolean existsByUsername(String username);

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByUsernameAndIdNot(String username, Integer id);

  boolean existsByUsernameIgnoreCaseAndIdNot(String username, Integer id);
}
