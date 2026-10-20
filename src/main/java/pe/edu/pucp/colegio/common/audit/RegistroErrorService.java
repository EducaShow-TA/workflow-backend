package pe.edu.pucp.colegio.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste en {@code registros_error} las fallas técnicas del sistema (US-5.5). Usa una transacción
 * propia para que el registro se guarde aunque la petición original haga rollback. Si el registro
 * falla, no se propaga la excepción: el error original siempre debe llegar al cliente.
 */
@Service
public class RegistroErrorService {

  private static final Logger log = LoggerFactory.getLogger(RegistroErrorService.class);
  private static final int MAX = 255;

  private final RegistroErrorRepository errores;

  public RegistroErrorService(RegistroErrorRepository errores) {
    this.errores = errores;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registrar(
      NivelLog nivel, String codigo, String metodo, String endpoint, int estado, String mensaje) {
    try {
      errores.save(
          new RegistroError(
              nivel, codigo, metodo, recortar(endpoint), estado, recortar(mensaje)));
    } catch (RuntimeException e) {
      log.error("event=registro_error_fallido codigo={} causa={}", codigo, e.toString());
    }
  }

  private static String recortar(String texto) {
    return texto == null || texto.length() <= MAX ? texto : texto.substring(0, MAX);
  }
}
