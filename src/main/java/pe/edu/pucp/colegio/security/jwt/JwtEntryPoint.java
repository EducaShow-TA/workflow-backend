package pe.edu.pucp.colegio.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import pe.edu.pucp.colegio.common.audit.NivelLog;
import pe.edu.pucp.colegio.common.audit.RegistroErrorService;
import pe.edu.pucp.colegio.common.exception.ErrorRespuestaDTO;

@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(JwtEntryPoint.class);

  private final RegistroErrorService registroError;
  private final ObjectMapper json;

  public JwtEntryPoint(RegistroErrorService registroError, ObjectMapper json) {
    this.registroError = registroError;
    this.json = json;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    var cuerpo =
        ErrorRespuestaDTO.de(HttpStatus.UNAUTHORIZED, "No autenticado", request.getRequestURI());
    try {
      registroError.registrar(
          NivelLog.ADVERTENCIA,
          "AUTH_401",
          request.getMethod(),
          request.getRequestURI(),
          401,
          cuerpo.message(),
          null);
    } catch (RuntimeException ex) {
      log.error("event=registro_error_no_persistido codigo={}", "AUTH_401", ex);
    }
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    json.writeValue(response.getWriter(), cuerpo);
  }
}
