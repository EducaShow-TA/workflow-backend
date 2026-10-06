package pe.edu.pucp.colegio.seguridad.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import pe.edu.pucp.colegio.common.exception.ErrorRespuestaDTO;

@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    var cuerpo = ErrorRespuestaDTO.de(HttpStatus.UNAUTHORIZED, "No autenticado", request.getRequestURI());
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                cuerpo.timestamp(),
                cuerpo.status(),
                cuerpo.error(),
                cuerpo.message(),
                cuerpo.path()));
  }
}
