package pe.edu.pucp.colegio.common.validation;

import java.io.IOException;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.exception.BusinessException;

/** Comprobaciones comunes de los archivos CSV que llegan por carga masiva. */
public final class ArchivoCsv {

  private ArchivoCsv() {}

  /** Devuelve el contenido del archivo o rechaza el envío si falta, está vacío o no es .csv. */
  public static byte[] leerContenido(MultipartFile archivo) throws IOException {
    if (archivo == null || archivo.isEmpty()) {
      throw new BusinessException("Debe adjuntar un archivo CSV en el campo 'archivo'");
    }
    String nombre = archivo.getOriginalFilename();
    if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new BusinessException("El archivo debe tener extensión .csv");
    }
    return archivo.getBytes();
  }
}
