package pe.edu.pucp.colegio.common.service;

import java.util.Locale;
import pe.edu.pucp.colegio.common.model.NivelEducativo;

/** Formato único de grado y sección en documentos y mensajes (ortografía de la RAE: 1.º). */
public final class NombreSeccion {

  private NombreSeccion() {}

  /** «3.º de secundaria». */
  public static String grado(Integer numero, String nivel) {
    String nombreNivel = nivel == null ? "secundaria" : nivel.toLowerCase(Locale.ROOT);
    return numero + ".º de " + nombreNivel;
  }

  public static String grado(Integer numero, NivelEducativo nivel) {
    return grado(numero, nivel == null ? null : nivel.name());
  }

  /** «3.º de secundaria A». */
  public static String completa(Integer numero, String seccion, String nivel) {
    return grado(numero, nivel) + " " + seccion;
  }

  public static String completa(Integer numero, String seccion, NivelEducativo nivel) {
    return grado(numero, nivel) + " " + seccion;
  }

  public static String completa(Integer numero, String seccion) {
    return completa(numero, seccion, (String) null);
  }
}
