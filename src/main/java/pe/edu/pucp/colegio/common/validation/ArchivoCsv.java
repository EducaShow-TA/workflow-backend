package pe.edu.pucp.colegio.common.validation;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.exception.BusinessException;

/** Comprobaciones comunes de los archivos CSV que llegan por carga masiva. */
public final class ArchivoCsv {

  private ArchivoCsv() {}

  /** Devuelve CSV normalizado. También convierte el primer libro Excel .xls/.xlsx a CSV. */
  public static byte[] leerContenido(MultipartFile archivo) throws IOException {
    if (archivo == null || archivo.isEmpty()) {
      throw new BusinessException("Debe adjuntar un archivo CSV o Excel en el campo 'archivo'");
    }
    String nombre = archivo.getOriginalFilename();
    if (nombre == null) {
      throw new BusinessException("El archivo no tiene un nombre válido");
    }
    String nombreLower = nombre.toLowerCase(Locale.ROOT);
    if (nombreLower.endsWith(".csv")) {
      return archivo.getBytes();
    }
    if (!nombreLower.endsWith(".xls") && !nombreLower.endsWith(".xlsx")) {
      throw new BusinessException("El archivo debe tener extensión .csv, .xls o .xlsx");
    }
    try (Workbook libro = WorkbookFactory.create(archivo.getInputStream())) {
      if (libro.getNumberOfSheets() == 0) {
        throw new BusinessException("El archivo Excel no contiene hojas");
      }
      Sheet hoja = libro.getSheetAt(0);
      if (hoja.getPhysicalNumberOfRows() == 0) {
        throw new BusinessException("El archivo Excel no contiene filas");
      }
      StringWriter salida = new StringWriter();
      DataFormatter formato = new DataFormatter(Locale.forLanguageTag("es-PE"));
      try (CSVPrinter impresora =
          new CSVPrinter(salida, CSVFormat.DEFAULT.builder().setDelimiter(',').build())) {
        int ultima = hoja.getLastRowNum();
        for (int indice = 0; indice <= ultima; indice++) {
          Row fila = hoja.getRow(indice);
          int ultimaCelda = fila == null ? -1 : fila.getLastCellNum();
          Object[] valores = new Object[Math.max(0, ultimaCelda)];
          for (int celda = 0; celda < ultimaCelda; celda++) {
            valores[celda] =
                fila == null || fila.getCell(celda) == null
                    ? ""
                    : formato.formatCellValue(fila.getCell(celda));
          }
          impresora.printRecord(valores);
        }
      }
      return salida.toString().getBytes(StandardCharsets.UTF_8);
    } catch (Exception excepcion) {
      throw new BusinessException("No se pudo leer el archivo Excel");
    }
  }
}
