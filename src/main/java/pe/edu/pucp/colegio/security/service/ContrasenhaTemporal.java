package pe.edu.pucp.colegio.security.service;

import java.security.SecureRandom;

/** Genera contraseñas iniciales que cumplen la política de seguridad de las cuentas. */
public final class ContrasenhaTemporal {

  private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
  private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
  private static final String DIGITOS = "23456789";
  private static final String SIMBOLOS = "!@#$%&*?";
  private static final String ALFABETO = MAYUSCULAS + MINUSCULAS + DIGITOS + SIMBOLOS;
  private static final String[] GRUPOS_REQUERIDOS = {MAYUSCULAS, MINUSCULAS, DIGITOS, SIMBOLOS};
  private static final int LONGITUD = 12;
  private static final SecureRandom ALEATORIO = new SecureRandom();

  private ContrasenhaTemporal() {}

  public static String generar() {
    char[] clave = new char[LONGITUD];
    for (int i = 0; i < GRUPOS_REQUERIDOS.length; i++) {
      String grupo = GRUPOS_REQUERIDOS[i];
      clave[i] = grupo.charAt(ALEATORIO.nextInt(grupo.length()));
    }
    for (int i = GRUPOS_REQUERIDOS.length; i < clave.length; i++) {
      clave[i] = ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length()));
    }
    for (int i = clave.length - 1; i > 0; i--) {
      int j = ALEATORIO.nextInt(i + 1);
      char temporal = clave[i];
      clave[i] = clave[j];
      clave[j] = temporal;
    }
    return new String(clave);
  }
}
