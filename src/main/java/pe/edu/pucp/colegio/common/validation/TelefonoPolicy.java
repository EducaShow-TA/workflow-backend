package pe.edu.pucp.colegio.common.validation;

import java.util.regex.Pattern;

/** Formato internacional simplificado: signo + opcional y entre 7 y 15 dígitos. */
public final class TelefonoPolicy {

  public static final String PATRON = "\\+?[1-9][0-9]{6,14}";
  private static final Pattern FORMATO = Pattern.compile(PATRON);

  private TelefonoPolicy() {}

  public static boolean esValido(String telefono) {
    return telefono != null && FORMATO.matcher(telefono).matches();
  }
}
