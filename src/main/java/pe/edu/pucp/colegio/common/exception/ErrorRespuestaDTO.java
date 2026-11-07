package pe.edu.pucp.colegio.common.exception;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;

/** Formato estándar de error del Estándar de Programación, sección 6.3. */
public record ErrorRespuestaDTO(
    String timestamp, int status, String error, String message, String path) {

  private static final DateTimeFormatter FORMATO =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  public static ErrorRespuestaDTO de(HttpStatus estado, String mensaje, String ruta) {
    return new ErrorRespuestaDTO(
        OffsetDateTime.now().format(FORMATO),
        estado.value(),
        estado.getReasonPhrase(),
        mensaje,
        ruta);
  }
}
