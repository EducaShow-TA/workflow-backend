package pe.edu.pucp.colegio.common.dto;

/**
 * Resultado de verificar un documento antes de vincularlo como apoderado. Solo incluye nombre y
 * contacto enmascarado cuando la persona ya es apoderado y se puede vincular o ya está vinculada.
 */
public record VerificacionApoderadoResponse(
    boolean existe,
    boolean puedeVincularse,
    String motivo,
    String nombreCompleto,
    String celularEnmascarado,
    String correoEnmascarado,
    boolean tieneCuenta) {

  public static VerificacionApoderadoResponse nueva() {
    return new VerificacionApoderadoResponse(false, true, null, null, null, null, false);
  }

  public static VerificacionApoderadoResponse rechazada(boolean existe, String motivo) {
    return new VerificacionApoderadoResponse(existe, false, motivo, null, null, null, false);
  }
}
