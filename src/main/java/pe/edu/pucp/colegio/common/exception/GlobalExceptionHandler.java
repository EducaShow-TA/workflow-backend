package pe.edu.pucp.colegio.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(UnauthorizedException.class)
  ResponseEntity<Map<String, String>> unauthorized(UnauthorizedException e, HttpServletRequest r) {
    log.warn("event=authentication_rejected path={} reason={}", r.getRequestURI(), e.getMessage());
    return response(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
    return response(
        HttpStatus.BAD_REQUEST, e.getBindingResult().getFieldError().getDefaultMessage());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, String>> unexpected(Exception e, HttpServletRequest r) {
    log.error("event=unhandled_exception path={}", r.getRequestURI(), e);
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno");
  }

  private ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of("message", message));
  }
}
