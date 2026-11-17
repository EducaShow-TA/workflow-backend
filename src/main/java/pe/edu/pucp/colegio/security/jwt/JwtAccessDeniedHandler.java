package pe.edu.pucp.colegio.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import pe.edu.pucp.colegio.common.exception.ErrorRespuestaDTO;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
  private static final String MENSAJE = "No tiene permiso para este recurso";

  private final ObjectMapper json;

  public JwtAccessDeniedHandler(ObjectMapper json) {
    this.json = json;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    log.warn("event=access_denied path={}", request.getRequestURI());
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    json.writeValue(
        response.getWriter(),
        ErrorRespuestaDTO.de(HttpStatus.FORBIDDEN, MENSAJE, request.getRequestURI()));
  }
}
