package pe.edu.pucp.colegio.seguridad.jwt;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.pucp.colegio.seguridad.model.CuentaUsuario;
import pe.edu.pucp.colegio.seguridad.repository.CuentaUsuarioRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokens;
  private final CuentaUsuarioRepository users;

  public JwtAuthenticationFilter(JwtTokenProvider tokens, CuentaUsuarioRepository users) {
    this.tokens = tokens;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      if (tokens.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
        users
            .findByUsername(tokens.getUsername(token))
            .filter(u -> u.getActive() && !u.estaBloqueada())
            .ifPresent(this::authenticate);
      }
    }
    chain.doFilter(request, response);
  }

  private void authenticate(CuentaUsuario user) {
    var auth =
        new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRol())));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
