package pe.edu.pucp.colegio.security.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.service.CorreoService;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.TokenRecuperacionPassword;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.repository.TokenRecuperacionPasswordRepository;
import pe.edu.pucp.colegio.security.service.RecuperacionPasswordService;

@Service
public class RecuperacionPasswordServiceImpl implements RecuperacionPasswordService {

  private static final Logger log = LoggerFactory.getLogger(RecuperacionPasswordServiceImpl.class);

  private static final SecureRandom RANDOM = new SecureRandom();

  private final CuentaUsuarioRepository cuentas;
  private final TokenRecuperacionPasswordRepository tokens;
  private final PasswordEncoder passwordEncoder;
  private final CorreoService correo;
  private final RegistroAuditoriaRepository auditoria;

  private final String frontendUrl;
  private final long expirationMinutes;

  public RecuperacionPasswordServiceImpl(
      CuentaUsuarioRepository cuentas,
      TokenRecuperacionPasswordRepository tokens,
      PasswordEncoder passwordEncoder,
      CorreoService correo,
      RegistroAuditoriaRepository auditoria,
      @Value("${app.password-reset.frontend-url}") String frontendUrl,
      @Value("${app.password-reset.expiration-minutes:15}") long expirationMinutes) {

    this.cuentas = cuentas;
    this.tokens = tokens;
    this.passwordEncoder = passwordEncoder;
    this.correo = correo;
    this.auditoria = auditoria;
    this.frontendUrl = frontendUrl;
    this.expirationMinutes = expirationMinutes;
  }

  @Override
  @Transactional
  public void solicitarRecuperacion(String username) {
    if (username == null || username.isBlank()) {
      return;
    }

    CuentaUsuario cuenta =
        cuentas
            .findByUsername(username.trim())
            .filter(c -> Boolean.TRUE.equals(c.getActive()))
            .orElse(null);

    // No revelar si una cuenta existe o está activa.
    if (cuenta == null) {
      return;
    }

    // Invalidar solicitudes anteriores aún activas.
    List<TokenRecuperacionPassword> anteriores = tokens.findByCuentaUsuarioAndActiveTrue(cuenta);

    anteriores.forEach(t -> t.setActive(false));

    // El token original solo se entrega al usuario por correo.
    String tokenClaro = generarToken();
    String tokenHash = calcularHash(tokenClaro);

    LocalDateTime ahora = LocalDateTime.now();

    TokenRecuperacionPassword token =
        new TokenRecuperacionPassword(
            cuenta, tokenHash, ahora, ahora.plusMinutes(expirationMinutes));

    tokens.save(token);

    String separador = frontendUrl.contains("?") ? "&" : "?";
    String enlace =
        frontendUrl + separador + "token=" + URLEncoder.encode(tokenClaro, StandardCharsets.UTF_8);

    try {
      correo.enviarCorreoRecuperacion(cuenta.getUsername(), enlace, expirationMinutes);
    } catch (MailException e) {
      // No dejar un token activo que el usuario no ha recibido.
      token.setActive(false);
      tokens.save(token);

      // No registrar el token ni información sensible del correo.
      log.warn(
          "event=password_reset_email_failed cuentaId={} tipoError={}",
          cuenta.getId(),
          e.getClass().getSimpleName());
    }
  }

  @Override
  @Transactional
  public void restablecerPassword(String tokenClaro, String nuevaPassword) {

    if (tokenClaro == null || tokenClaro.isBlank()) {
      throw tokenInvalido();
    }

    String hash = calcularHash(tokenClaro);

    TokenRecuperacionPassword token =
        tokens.findByTokenHashAndActiveTrueAndUsadoEnIsNull(hash).orElseThrow(this::tokenInvalido);

    LocalDateTime ahora = LocalDateTime.now();
    CuentaUsuario cuenta = token.getCuentaUsuario();

    if (!token.getExpiraEn().isAfter(ahora) || !Boolean.TRUE.equals(cuenta.getActive())) {
      throw tokenInvalido();
    }

    // Mantener el mismo algoritmo utilizado por el login.
    cuenta.setPassword(passwordEncoder.encode(nuevaPassword));
    cuenta.reiniciarIntentos();
    cuentas.save(cuenta);
    auditoria.save(
        new RegistroAuditoria(
            cuenta.getId(),
            cuenta.getRol().name(),
            AccionAuditoria.ACTUALIZAR,
            "Contraseña restablecida"));

    // Invalidar todos los enlaces activos de esta cuenta.
    List<TokenRecuperacionPassword> activos = tokens.findByCuentaUsuarioAndActiveTrue(cuenta);

    for (TokenRecuperacionPassword actual : activos) {
      actual.setActive(false);

      if (actual.getId().equals(token.getId())) {
        actual.setUsadoEn(ahora);
      }
    }

    tokens.saveAll(activos);
  }

  private String generarToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String calcularHash(String token) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No se pudo calcular el hash del token", e);
    }
  }

  private ResponseStatusException tokenInvalido() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "El enlace de recuperación no es válido o ha expirado");
  }
}
