package pe.edu.pucp.colegio.common.service;

import pe.edu.pucp.colegio.common.exception.BusinessException;

final class MotivoRectificacion {

  static final int LONGITUD_MINIMA = 10;

  private MotivoRectificacion() {}

  static String exigir(String motivo) {
    String limpio = motivo == null ? "" : motivo.trim();
    if (limpio.length() < LONGITUD_MINIMA) {
      throw new BusinessException(
          "Indica el motivo de la rectificación (mínimo " + LONGITUD_MINIMA + " caracteres)");
    }
    return limpio;
  }
}
