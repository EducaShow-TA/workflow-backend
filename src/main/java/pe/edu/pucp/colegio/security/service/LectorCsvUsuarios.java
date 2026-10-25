package pe.edu.pucp.colegio.security.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;

/** Convierte el contenido de un CSV de usuarios en filas (solo comprueba la estructura). */
@Component
public class LectorCsvUsuarios {

  public static final int MAX_FILAS = 500;

  private static final List<String> COLUMNAS_OBLIGATORIAS =
      List.of("rol", "dni", "nombres", "apellido_paterno", "apellido_materno", "correo");

  public List<FilaUsuarioCsv> leer(byte[] contenido) {
    String texto = new String(contenido, StandardCharsets.UTF_8);
    if (texto.indexOf('\uFFFD') >= 0) {
      texto = new String(contenido, Charset.forName("windows-1252"));
    }
    if (texto.startsWith("\uFEFF")) {
      texto = texto.substring(1);
    }
    if (texto.isBlank()) {
      throw new BusinessException("El archivo CSV está vacío");
    }
    CSVFormat formato =
        CSVFormat.DEFAULT
            .builder()
            .setDelimiter(detectarSeparador(texto))
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
    try (CSVParser lector = formato.parse(new StringReader(texto))) {
      validarCabecera(lector.getHeaderNames());
      List<FilaUsuarioCsv> filas = new ArrayList<>();
      int fila = 0;
      for (CSVRecord registro : lector) {
        fila++;
        if (fila > MAX_FILAS) {
          throw new BusinessException("El archivo supera el máximo de " + MAX_FILAS + " filas");
        }
        filas.add(
            new FilaUsuarioCsv(
                fila,
                valor(registro, "rol"),
                valor(registro, "dni"),
                valor(registro, "nombres"),
                valor(registro, "apellido_paterno"),
                valor(registro, "apellido_materno"),
                valor(registro, "correo"),
                valor(registro, "telefono"),
                valor(registro, "fecha_nacimiento"),
                valor(registro, "dni_apoderado"),
                valor(registro, "parentesco"),
                valor(registro, "username")));
      }
      if (filas.isEmpty()) {
        throw new BusinessException("El archivo no contiene filas de datos");
      }
      return filas;
    } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
      throw new BusinessException("El archivo CSV no tiene un formato válido");
    }
  }

  private static char detectarSeparador(String texto) {
    int fin = texto.indexOf('\n');
    String cabecera = fin < 0 ? texto : texto.substring(0, fin);
    long puntoYComa = cabecera.chars().filter(c -> c == ';').count();
    long comas = cabecera.chars().filter(c -> c == ',').count();
    return puntoYComa > comas ? ';' : ',';
  }

  private static void validarCabecera(List<String> cabecera) {
    List<String> presentes = cabecera.stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
    List<String> faltantes =
        COLUMNAS_OBLIGATORIAS.stream().filter(c -> !presentes.contains(c)).toList();
    if (!faltantes.isEmpty()) {
      throw new BusinessException("Faltan columnas obligatorias: " + String.join(", ", faltantes));
    }
  }

  private static String valor(CSVRecord registro, String columna) {
    if (!registro.isMapped(columna) || !registro.isSet(columna)) {
      return null;
    }
    String valor = registro.get(columna);
    return valor == null || valor.isBlank() ? null : valor.trim();
  }
}
