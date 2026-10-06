package pe.edu.pucp.colegio.common.exception;

/** Conflicto de negocio (por ejemplo, un duplicado). Se traduce a HTTP 409. */
public class ConflictException extends BusinessException {

  public ConflictException(String message) {
    super(message);
  }
}
