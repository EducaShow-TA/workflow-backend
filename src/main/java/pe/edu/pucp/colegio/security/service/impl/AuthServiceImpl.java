package pe.edu.pucp.colegio.security.service.impl;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.audit.*;
import pe.edu.pucp.colegio.common.exception.UnauthorizedException;
import pe.edu.pucp.colegio.security.dto.*;
import pe.edu.pucp.colegio.security.jwt.JwtTokenProvider;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {

  private final CuentaUsuarioRepository users;
  private final RegistroAuditoriaRepository audits;
  private final PasswordEncoder encoder;
  private final JwtTokenProvider tokens;
  private final int maxAttempts, lockMinutes;
  private final String hashPasswordDummy;

  public AuthServiceImpl(
      CuentaUsuarioRepository users,
      RegistroAuditoriaRepository audits,
      PasswordEncoder encoder,
      JwtTokenProvider tokens,
      @Value("${app.security.max-login-attempts}") int maxAttempts,
      @Value("${app.security.lock-time-duration-minutes}") int lockMinutes) {
    this.users = users;
    this.audits = audits;
    this.encoder = encoder;
    this.tokens = tokens;
    this.maxAttempts = maxAttempts;
    this.lockMinutes = lockMinutes;
    this.hashPasswordDummy = encoder.encode(UUID.randomUUID().toString());
  }

  @Transactional(noRollbackFor = UnauthorizedException.class)
  public AuthResponseDTO login(LoginRequestDTO request) {
    CuentaUsuario user = users.findByUsername(request.getUsername()).orElse(null);
    if (user == null || !Boolean.TRUE.equals(user.getActive()) || user.estaBloqueada()) {
      encoder.matches(request.getPassword(), hashPasswordDummy);
      throw new UnauthorizedException("Credenciales inválidas");
    }
    if (user.bloqueoVencido()) {
      user.reiniciarIntentos();
    }
    if (!encoder.matches(request.getPassword(), user.getPassword())) {
      user.registrarIntentoFallido(maxAttempts, lockMinutes);
      users.save(user);
      throw new UnauthorizedException("Credenciales inválidas");
    }
    user.reiniciarIntentos();
    users.save(user);
    audits.save(
        new RegistroAuditoria(
            user.getId(),
            user.getRol().name(),
            AccionAuditoria.INICIAR_SESION,
            "Inicio de sesión exitoso"));
    return new AuthResponseDTO(
        tokens.generateToken(user), "Bearer", tokens.getExpirationInSeconds(), toProfile(user));
  }

  @Transactional(readOnly = true)
  public UsuarioPerfilDTO currentUser(String username) {
    return toProfile(
        users
            .findByUsername(username)
            .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado")));
  }

  private UsuarioPerfilDTO toProfile(CuentaUsuario user) {
    var p = user.getPersona();
    return new UsuarioPerfilDTO(
        user.getId(),
        user.getUsername(),
        user.getRol().name(),
        p == null ? user.getUsername() : p.getNombreCompleto(),
        p == null ? "" : p.getDni());
  }
}
