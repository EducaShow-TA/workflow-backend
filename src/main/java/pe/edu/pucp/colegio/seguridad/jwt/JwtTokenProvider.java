package pe.edu.pucp.colegio.seguridad.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.pucp.colegio.seguridad.model.CuentaUsuario;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long expirationMs;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.expirationMs = expirationMs;
  }

  public String generateToken(CuentaUsuario user) {
    Date now = new Date();
    return Jwts.builder()
        .subject(user.getUsername())
        .claim("idUsuario", user.getId())
        .claim("rol", user.getRol().name())
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  public String getUsername(String token) {
    return parse(token).getSubject();
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public long getExpirationInSeconds() {
    return expirationMs / 1000;
  }
}
