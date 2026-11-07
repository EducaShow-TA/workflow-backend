package pe.edu.pucp.colegio.security.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.TokenRecuperacionPassword;

public interface TokenRecuperacionPasswordRepository
    extends JpaRepository<TokenRecuperacionPassword, Integer> {

  List<TokenRecuperacionPassword> findByCuentaUsuarioAndActiveTrue(CuentaUsuario cuentaUsuario);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TokenRecuperacionPassword> findByTokenHashAndActiveTrueAndUsadoEnIsNull(
      String tokenHash);
}
