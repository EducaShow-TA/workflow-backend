package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record EstudianteResponse(
    Integer estudianteId,
    Integer personaId,
    String username,
    String rol,
    String dni,
    String nombreCompleto,
    List<CuentaTemporalResponse> cuentasApoderados) {

  public EstudianteResponse(
      Integer estudianteId,
      Integer personaId,
      String username,
      String rol,
      String dni,
      String nombreCompleto) {
    this(estudianteId, personaId, username, rol, dni, nombreCompleto, List.of());
  }
}
