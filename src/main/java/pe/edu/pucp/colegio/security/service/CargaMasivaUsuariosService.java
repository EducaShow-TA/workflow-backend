package pe.edu.pucp.colegio.security.service;

import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;

/** Creación de cuentas de docentes, alumnos y apoderados a partir de un CSV (US-5.9). */
public interface CargaMasivaUsuariosService {

  /**
   * Valida todo el archivo y, solo si no hay errores, crea todas las cuentas en una sola
   * transacción. Si hay errores no se guarda nada y el resultado los detalla.
   */
  CargaMasivaResultadoDTO cargar(byte[] contenido, String usernameResponsable);
}
