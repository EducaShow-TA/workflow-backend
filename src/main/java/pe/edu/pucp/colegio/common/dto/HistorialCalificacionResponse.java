package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDateTime;
import pe.edu.pucp.colegio.common.audit.AuditoriaCalificacion;

public record HistorialCalificacionResponse(
    Integer id,
    LocalDateTime fechaHora,
    String usuario,
    String rolUsuario,
    String accion,
    String notaAnterior,
    String notaNueva,
    String motivo) {

  public static HistorialCalificacionResponse desde(AuditoriaCalificacion registro) {
    return new HistorialCalificacionResponse(
        registro.getId(),
        registro.getFechaHora(),
        registro.getUsuario(),
        registro.getRolUsuario(),
        registro.getAccion(),
        registro.getNotaAnterior(),
        registro.getNotaNueva(),
        registro.getMotivo());
  }
}
