package pe.edu.pucp.colegio.common.exception;

import java.util.List;
import pe.edu.pucp.colegio.common.dto.IncidenciaConfirmacionMatriculaResponse;

/** Rechazo atómico de una confirmación masiva con todos los problemas encontrados. */
public class ValidacionConfirmacionMatriculaException extends RuntimeException {

  private final List<IncidenciaConfirmacionMatriculaResponse> incidencias;

  public ValidacionConfirmacionMatriculaException(
      List<IncidenciaConfirmacionMatriculaResponse> incidencias) {
    super("No se confirmaron las matrículas. Revisa los problemas indicados por estudiante.");
    this.incidencias = List.copyOf(incidencias);
  }

  public List<IncidenciaConfirmacionMatriculaResponse> getIncidencias() {
    return incidencias;
  }
}
