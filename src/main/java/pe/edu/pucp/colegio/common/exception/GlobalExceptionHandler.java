package pe.edu.pucp.colegio.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.pucp.colegio.common.audit.NivelLog;
import pe.edu.pucp.colegio.common.audit.RegistroErrorService;

/**
 * Traduce las excepciones a respuestas HTTP con el formato estándar (sección 6.3) y registra las
 * fallas en el archivo de log y en la tabla {@code registros_error}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final RegistroErrorService registroError;

  public GlobalExceptionHandler(RegistroErrorService registroError) {
    this.registroError = registroError;
  }

  @ExceptionHandler(UnauthorizedException.class)
  ResponseEntity<ErrorRespuestaDTO> noAutenticado(UnauthorizedException e, HttpServletRequest r) {
    log.warn("event=authentication_rejected path={} reason={}", r.getRequestURI(), e.getMessage());
    return responder(HttpStatus.UNAUTHORIZED, NivelLog.ADVERTENCIA, "AUTH_401", e.getMessage(), r);
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<ErrorRespuestaDTO> conflicto(ConflictException e, HttpServletRequest r) {
    log.warn("event=business_conflict path={} reason={}", r.getRequestURI(), e.getMessage());
    return responder(HttpStatus.CONFLICT, NivelLog.ADVERTENCIA, "NEG_409", e.getMessage(), r);
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ErrorRespuestaDTO> negocio(BusinessException e, HttpServletRequest r) {
    log.warn("event=business_rule_failed path={} reason={}", r.getRequestURI(), e.getMessage());
    return responder(
        HttpStatus.UNPROCESSABLE_ENTITY, NivelLog.ADVERTENCIA, "NEG_422", e.getMessage(), r);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorRespuestaDTO> invalido(
      MethodArgumentNotValidException e, HttpServletRequest r) {
    var campo = e.getBindingResult().getFieldError();
    String mensaje = campo == null ? "Solicitud inválida" : campo.getDefaultMessage();
    return responder(HttpStatus.BAD_REQUEST, NivelLog.INFO, "VAL_400", mensaje, r);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorRespuestaDTO> malformado(HttpMessageNotReadableException e,
      HttpServletRequest r) {
    return responder(HttpStatus.BAD_REQUEST, NivelLog.INFO, "REQ_400", "JSON inválido", r);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorRespuestaDTO> prohibido(AccessDeniedException e, HttpServletRequest r) {
    log.warn("event=access_denied path={}", r.getRequestURI());
    return responder(
        HttpStatus.FORBIDDEN, NivelLog.ADVERTENCIA, "AUTH_403", "No tiene permiso para este recurso", r);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorRespuestaDTO> inesperado(Exception e, HttpServletRequest r) {
    log.error("event=unhandled_exception path={}", r.getRequestURI(), e);
    return responder(
        HttpStatus.INTERNAL_SERVER_ERROR, NivelLog.ERROR, "SIS_500", "Ocurrió un error interno", r);
  }

  private ResponseEntity<ErrorRespuestaDTO> responder(
      HttpStatus estado, NivelLog nivel, String codigo, String mensaje, HttpServletRequest r) {
    registroError.registrar(
        nivel, codigo, r.getMethod(), r.getRequestURI(), estado.value(), mensaje);
    return ResponseEntity.status(estado)
        .body(ErrorRespuestaDTO.de(estado, mensaje, r.getRequestURI()));
  }
}
