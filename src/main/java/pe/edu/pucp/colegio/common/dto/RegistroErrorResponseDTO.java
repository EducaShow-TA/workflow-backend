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
    String usuario,
    String rolUsuario,
    String metodoHttp,
    String endpoint,
    Integer codigoEstado,
    String mensaje,
    String stackTrace) {

  /**
   * Sin la traza técnica; usuario y rol son nulos si la petición fue anónima o la cuenta ya no
   * existe.
   */
  public static RegistroErrorResponseDTO resumen(RegistroError e, String usuario, String rol) {
    return de(e, usuario, rol, null);
  }

  public static RegistroErrorResponseDTO detalle(RegistroError e, String usuario, String rol) {
    return de(e, usuario, rol, e.getStackTrace());
  }

  private static RegistroErrorResponseDTO de(
      RegistroError e, String usuario, String rol, String traza) {
    return new RegistroErrorResponseDTO(
        e.getId(),
        e.getFechaHora(),
        e.getNivel(),
        e.getCodigoError(),
        e.getModulo(),
        e.getIdUsuario(),
        usuario,
        rol,
        e.getMetodoHttp(),
        e.getEndpoint(),
        e.getCodigoEstado(),
        e.getMensaje(),
        traza);
  }
}
