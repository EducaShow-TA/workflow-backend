package pe.edu.pucp.colegio.common.validation;

import java.util.regex.Pattern;

/** Reglas compartidas para los nombres de usuario, separados de los correos de contacto. */
public final class UsernamePolicy {

  public static final String PATRON = "^[A-Za-z0-9]+$";
  private static final Pattern FORMATO = Pattern.compile(PATRON);

  private UsernamePolicy() {}

  public static boolean esValido(String username) {
    return username != null && FORMATO.matcher(username).matches();
  }

  /** Genera un usuario determinista y alfanumérico para las filas CSV sin username explícito. */
  public static String generarDesdeDocumento(String documento) {
    return "usuario" + (documento == null ? "" : documento.replaceAll("[^A-Za-z0-9]", ""));
  }
}
