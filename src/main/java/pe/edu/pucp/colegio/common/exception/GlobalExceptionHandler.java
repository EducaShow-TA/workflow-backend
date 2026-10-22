package pe.edu.pucp.colegio.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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
  ResponseEntity<ErrorRespuestaDTO> malformado(
      HttpMessageNotReadableException e, HttpServletRequest r) {
    return responder(HttpStatus.BAD_REQUEST, NivelLog.INFO, "REQ_400", "JSON inválido", r);
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ErrorRespuestaDTO> estadoSolicitado(
      ResponseStatusException e, HttpServletRequest r) {
    HttpStatus estado =
        HttpStatus.resolve(e.getStatusCode().value()) == null
            ? HttpStatus.INTERNAL_SERVER_ERROR
            : HttpStatus.resolve(e.getStatusCode().value());
    NivelLog nivel =
        estado.is5xxServerError()
            ? NivelLog.ERROR
            : estado == HttpStatus.CONFLICT
                    || estado == HttpStatus.UNAUTHORIZED
                    || estado == HttpStatus.FORBIDDEN
                ? NivelLog.ADVERTENCIA
                : NivelLog.INFO;
    String codigo =
        switch (estado) {
          case BAD_REQUEST -> "REQ_400";
          case UNAUTHORIZED -> "AUTH_401";
          case FORBIDDEN -> "AUTH_403";
          case NOT_FOUND -> "RES_404";
          case CONFLICT -> "NEG_409";
          case UNPROCESSABLE_ENTITY -> "NEG_422";
          default -> "HTTP_" + estado.value();
        };
    String mensaje = e.getReason() == null ? estado.getReasonPhrase() : e.getReason();
    return responder(estado, nivel, codigo, mensaje, r);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorRespuestaDTO> prohibido(AccessDeniedException e, HttpServletRequest r) {
    log.warn("event=access_denied path={}", r.getRequestURI());
    return responder(
        HttpStatus.FORBIDDEN,
        NivelLog.ADVERTENCIA,
        "AUTH_403",
        "No tiene permiso para este recurso",
        r);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorRespuestaDTO> inesperado(Exception e, HttpServletRequest r) {
    log.error("event=unhandled_exception path={}", r.getRequestURI(), e);
    return responder(
        HttpStatus.INTERNAL_SERVER_ERROR,
        NivelLog.ERROR,
        "SIS_500",
        "Ocurrió un error interno",
        r,
        e);
  }

  /*
  private ResponseEntity<ErrorRespuestaDTO> responder(
          HttpStatus estado, NivelLog nivel, String codigo, String mensaje, HttpServletRequest r) {
      registroError.registrar(
              nivel, codigo, r.getMethod(), r.getRequestURI(), estado.value(), mensaje);
      return ResponseEntity.status(estado)
              .body(ErrorRespuestaDTO.de(estado, mensaje, r.getRequestURI()));
  }
  */

  private ResponseEntity<ErrorRespuestaDTO> responder(
      HttpStatus estado, NivelLog nivel, String codigo, String mensaje, HttpServletRequest r) {
    return responder(estado, nivel, codigo, mensaje, r, null);
  }

  private ResponseEntity<ErrorRespuestaDTO> responder(
      HttpStatus estado,
      NivelLog nivel,
      String codigo,
      String mensaje,
      HttpServletRequest r,
      Throwable causa) {
    try {
      registroError.registrar(
          nivel, codigo, r.getMethod(), r.getRequestURI(), estado.value(), mensaje, causa);
    } catch (RuntimeException ex) {
      log.error("event=registro_error_no_persistido codigo={}", codigo, ex);
    }
    return ResponseEntity.status(estado)
        .body(ErrorRespuestaDTO.de(estado, mensaje, r.getRequestURI()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorRespuestaDTO> noEncontrado(NoResourceFoundException e, HttpServletRequest r) {
    return responder(HttpStatus.NOT_FOUND, NivelLog.INFO, "RES_404", "Recurso no encontrado", r);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorRespuestaDTO> metodoNoPermitido(
      HttpRequestMethodNotSupportedException e, HttpServletRequest r) {
    return responder(
        HttpStatus.METHOD_NOT_ALLOWED, NivelLog.INFO, "REQ_405", "Método no permitido", r);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErrorRespuestaDTO> tipoNoSoportado(
      HttpMediaTypeNotSupportedException e, HttpServletRequest r) {
    return responder(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        NivelLog.INFO,
        "REQ_415",
        "Tipo de contenido no soportado",
        r);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ErrorRespuestaDTO> parametroInvalido(Exception e, HttpServletRequest r) {
    return responder(
        HttpStatus.BAD_REQUEST, NivelLog.INFO, "REQ_400", "Parámetro de la solicitud inválido", r);
  }
}
