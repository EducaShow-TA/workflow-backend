package pe.edu.pucp.colegio.common.service;

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
import pe.edu.pucp.colegio.common.dto.FilaAsistenciaDocenteCsv;
import pe.edu.pucp.colegio.common.dto.FilaAsistenciaEstudianteCsv;
import pe.edu.pucp.colegio.common.exception.BusinessException;

/** Convierte un CSV de asistencia en filas (solo comprueba la estructura, no los datos). */
@Component
public class LectorCsvAsistencia {

  public static final int MAX_FILAS = 500;

  private static final List<String> COLUMNAS_DOCENTE = List.of("dni_docente", "fecha");
  private static final List<String> COLUMNAS_ALUMNO = List.of("dni_alumno", "estado");

  /** US-2.5: obligatorias {@code dni_docente} y {@code fecha}; el resto es opcional. */
  public List<FilaAsistenciaDocenteCsv> leerDocentes(byte[] contenido) {
    List<FilaAsistenciaDocenteCsv> filas = new ArrayList<>();
    int fila = 0;
    for (CSVRecord registro : leer(contenido, COLUMNAS_DOCENTE)) {
      fila++;
      filas.add(
          new FilaAsistenciaDocenteCsv(
              fila,
              valor(registro, "dni_docente"),
              valor(registro, "fecha"),
              valor(registro, "hora_entrada"),
              valor(registro, "hora_salida"),
              valor(registro, "estado"),
              valor(registro, "motivo")));
    }
    return filas;
  }

  /** US-2.2: obligatorias {@code dni_alumno} y {@code estado}; otras columnas se ignoran. */
  public List<FilaAsistenciaEstudianteCsv> leerAlumnos(byte[] contenido) {
    List<FilaAsistenciaEstudianteCsv> filas = new ArrayList<>();
    int fila = 0;
    for (CSVRecord registro : leer(contenido, COLUMNAS_ALUMNO)) {
      fila++;
      filas.add(
          new FilaAsistenciaEstudianteCsv(
              fila, valor(registro, "dni_alumno"), valor(registro, "estado")));
    }
    return filas;
  }

  private List<CSVRecord> leer(byte[] contenido, List<String> obligatorias) {
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
      validarCabecera(lector.getHeaderNames(), obligatorias);
      List<CSVRecord> registros = new ArrayList<>();
      for (CSVRecord registro : lector) {
        if (registros.size() >= MAX_FILAS) {
          throw new BusinessException("El archivo supera el máximo de " + MAX_FILAS + " filas");
        }
        registros.add(registro);
      }
      if (registros.isEmpty()) {
        throw new BusinessException("El archivo no contiene filas de datos");
      }
      return registros;
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

  private static void validarCabecera(List<String> cabecera, List<String> obligatorias) {
    List<String> presentes = cabecera.stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
    List<String> faltantes = obligatorias.stream().filter(c -> !presentes.contains(c)).toList();
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
