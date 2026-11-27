package pe.edu.pucp.colegio.common.validation;

import java.time.LocalDate;
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

  /** Usuario de alumno asociado al año en que se matricula. */
  public static String generarAlumnoDesdeDocumento(String documento, int anhioMatricula) {
    String limpio = documento == null ? "" : documento.replaceAll("[^A-Za-z0-9]", "");
    return "usuario" + anhioMatricula + limpio;
  }

  public static String generarAlumnoDesdeDocumento(String documento) {
    return generarAlumnoDesdeDocumento(documento, LocalDate.now().getYear());
  }
}
