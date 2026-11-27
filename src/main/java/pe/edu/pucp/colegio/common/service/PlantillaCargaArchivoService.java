package pe.edu.pucp.colegio.common.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** Genera archivos de carga CSV y Excel con una primera hoja lista para importar. */
@Service
public class PlantillaCargaArchivoService {

  public String generarCsv(List<String> cabeceras, List<List<String>> filas) {
    try (StringWriter salida = new StringWriter();
        CSVPrinter impresora = new CSVPrinter(salida, CSVFormat.DEFAULT)) {
      impresora.printRecord(cabeceras);
      for (List<String> fila : filas) {
        impresora.printRecord(fila);
      }
      return salida.toString();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar la plantilla CSV", exception);
    }
  }

  public byte[] generarExcel(
      String titulo,
      String contexto,
      List<String> cabeceras,
      List<List<String>> filas,
      List<String> instrucciones) {
    try (Workbook libro = new XSSFWorkbook();
        ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
      Sheet carga = libro.createSheet("Carga");
      CellStyle estiloCabecera = libro.createCellStyle();
      estiloCabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
      estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      estiloCabecera.setBorderBottom(BorderStyle.THIN);
      Font fuenteCabecera = libro.createFont();
      fuenteCabecera.setBold(true);
      fuenteCabecera.setColor(IndexedColors.WHITE.getIndex());
      estiloCabecera.setFont(fuenteCabecera);

      Row encabezado = carga.createRow(0);
      for (int columna = 0; columna < cabeceras.size(); columna++) {
        var celda = encabezado.createCell(columna);
        celda.setCellValue(cabeceras.get(columna));
        celda.setCellStyle(estiloCabecera);
      }
      for (int indice = 0; indice < filas.size(); indice++) {
        Row fila = carga.createRow(indice + 1);
        List<String> valores = filas.get(indice);
        for (int columna = 0; columna < cabeceras.size(); columna++) {
          fila.createCell(columna)
              .setCellValue(columna < valores.size() ? valores.get(columna) : "");
        }
      }
      carga.createFreezePane(0, 1);
      if (!cabeceras.isEmpty()) {
        carga.setAutoFilter(
            new CellRangeAddress(0, Math.max(0, filas.size()), 0, cabeceras.size() - 1));
      }
      for (int columna = 0; columna < cabeceras.size(); columna++) {
        carga.autoSizeColumn(columna);
        carga.setColumnWidth(columna, Math.min(carga.getColumnWidth(columna) + 800, 14000));
      }

      Sheet guia = libro.createSheet("Guía");
      guia.createRow(0).createCell(0).setCellValue(titulo);
      guia.createRow(1).createCell(0).setCellValue(contexto);
      guia.createRow(3).createCell(0).setCellValue("Cómo completar la plantilla");
      for (int indice = 0; indice < instrucciones.size(); indice++) {
        guia.createRow(indice + 4).createCell(0).setCellValue(instrucciones.get(indice));
      }
      guia.setColumnWidth(0, 14000);

      libro.write(salida);
      return salida.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo generar la plantilla Excel", exception);
    }
  }
}
