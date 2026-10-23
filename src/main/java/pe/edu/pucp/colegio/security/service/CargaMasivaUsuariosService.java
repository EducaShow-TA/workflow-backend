package pe.edu.pucp.colegio.security.service;

import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;

/** Creación de cuentas de docentes, alumnos y apoderados a partir de un CSV (US-5.9). */
public interface CargaMasivaUsuariosService {

  /**
   * Valida cada fila y procesa las válidas, cada una en su propia transacción. Las filas rechazadas
   * se reportan con su motivo sin impedir el procesamiento del resto.
   */
  CargaMasivaResultadoDTO cargar(byte[] contenido, String usernameResponsable);
}
