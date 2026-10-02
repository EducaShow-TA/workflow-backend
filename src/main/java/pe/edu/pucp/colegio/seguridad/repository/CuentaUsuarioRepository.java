package pe.edu.pucp.colegio.seguridad.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.seguridad.model.CuentaUsuario;

public interface CuentaUsuarioRepository extends JpaRepository<CuentaUsuario, Integer> {

  Optional<CuentaUsuario> findByUsername(String username);
}
