package pe.edu.pucp.colegio.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import pe.edu.pucp.colegio.common.audit.NivelLog;
import pe.edu.pucp.colegio.common.audit.RegistroError;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegistroErrorResponseDTO(
    Integer id,
    LocalDateTime fechaHora,
    NivelLog nivel,
    String codigoError,
    String modulo,
    Integer idUsuario,
    String metodoHttp,
    String endpoint,
    Integer codigoEstado,
    String mensaje,
    String stackTrace) {

  public static RegistroErrorResponseDTO resumen(RegistroError e) {
    return de(e, null);
  }

  public static RegistroErrorResponseDTO detalle(RegistroError e) {
    return de(e, e.getStackTrace());
  }

  private static RegistroErrorResponseDTO de(RegistroError e, String traza) {
    return new RegistroErrorResponseDTO(
        e.getId(),
        e.getFechaHora(),
        e.getNivel(),
        e.getCodigoError(),
        e.getModulo(),
        e.getIdUsuario(),
        e.getMetodoHttp(),
        e.getEndpoint(),
        e.getCodigoEstado(),
        e.getMensaje(),
        traza);
  }
}
