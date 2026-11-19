package pe.edu.pucp.colegio.common.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.pucp.colegio.common.dto.CuadroMeritoResponse;
import pe.edu.pucp.colegio.common.dto.CuadroMeritoResponse.EstudianteRanking;

@Service
public class CuadroMeritoDocumentoService {

  private static final Color COLOR_INSTITUCIONAL = new Color(26, 72, 112);
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public byte[] generarPdf(CuadroMeritoResponse cuadro) {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    Document documento = new Document(PageSize.A4.rotate(), 36, 36, 38, 36);
    try {
      PdfWriter.getInstance(documento, salida);
      documento.open();
      Paragraph titulo =
          new Paragraph(
              "CUADRO DE MÉRITO", new Font(Font.HELVETICA, 18, Font.BOLD, COLOR_INSTITUCIONAL));
      titulo.setAlignment(Element.ALIGN_CENTER);
      documento.add(titulo);
      documento.add(
          new Paragraph(
              "Año escolar "
                  + cuadro.anhioEscolar()
                  + " · "
                  + cuadro.gradoNumero()
                  + "° "
                  + cuadro.nivelEducativo().toLowerCase()
                  + " · Sección "
                  + cuadro.seccion()));
      documento.add(
          new Paragraph(
              "Período: "
                  + cuadro.fechaInicio().format(FECHA)
                  + " al "
                  + cuadro.fechaFin().format(FECHA)
                  + " · Promedio de puntajes finales por competencia (C=1, B=2, A=3, AD=4)"));
      documento.add(new Paragraph(" "));

      PdfPTable tabla = new PdfPTable(new float[] {0.8f, 4.2f, 1.5f, 1.4f, 1.8f});
      tabla.setWidthPercentage(100);
      tabla.setHeaderRows(1);
      agregarEncabezado(tabla, "Puesto");
      agregarEncabezado(tabla, "Estudiante");
      agregarEncabezado(tabla, "ID estudiante");
      agregarEncabezado(tabla, "Competencias");
      agregarEncabezado(tabla, "Promedio");
      for (EstudianteRanking estudiante : cuadro.estudiantes()) {
        agregarCelda(tabla, valor(estudiante.puesto()));
        agregarCelda(tabla, estudiante.estudiante());
        agregarCelda(tabla, estudiante.estudianteId().toString());
        agregarCelda(tabla, Integer.toString(estudiante.competenciasCalificadas()));
        agregarCelda(tabla, valor(estudiante.promedio()));
      }
      documento.add(tabla);
      documento.close();
      return salida.toByteArray();
    } catch (DocumentException exception) {
      if (documento.isOpen()) {
        documento.close();
      }
      throw new IllegalStateException("No se pudo generar el PDF del cuadro de mérito", exception);
    }
  }

  public byte[] generarExcel(CuadroMeritoResponse cuadro) {
    try (Workbook libro = new XSSFWorkbook();
        ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
      Sheet hoja = libro.createSheet("Cuadro de mérito");
      Row titulo = hoja.createRow(0);
      titulo.createCell(0).setCellValue("CUADRO DE MÉRITO");
      Row datosPeriodo = hoja.createRow(1);
      datosPeriodo
          .createCell(0)
          .setCellValue(
              "Año escolar "
                  + cuadro.anhioEscolar()
                  + " · "
                  + cuadro.gradoNumero()
                  + "° "
                  + cuadro.nivelEducativo()
                  + " · Sección "
                  + cuadro.seccion());
      Row criterio = hoja.createRow(2);
      criterio
          .createCell(0)
          .setCellValue("Promedio de puntajes finales por competencia (C=1, B=2, A=3, AD=4)");

      Row encabezado = hoja.createRow(4);
      encabezado.createCell(0).setCellValue("Puesto");
      encabezado.createCell(1).setCellValue("Estudiante");
      encabezado.createCell(2).setCellValue("ID estudiante");
      encabezado.createCell(3).setCellValue("Competencias calificadas");
      encabezado.createCell(4).setCellValue("Promedio");

      CellStyle formatoPromedio = libro.createCellStyle();
      formatoPromedio.setDataFormat(libro.createDataFormat().getFormat("0.0000"));
      int fila = 5;
      for (EstudianteRanking estudiante : cuadro.estudiantes()) {
        Row registro = hoja.createRow(fila++);
        if (estudiante.puesto() != null) {
          registro.createCell(0).setCellValue(estudiante.puesto());
        } else {
          registro.createCell(0).setCellValue("Sin calificaciones");
        }
        registro.createCell(1).setCellValue(estudiante.estudiante());
        registro.createCell(2).setCellValue(estudiante.estudianteId());
        registro.createCell(3).setCellValue(estudiante.competenciasCalificadas());
        if (estudiante.promedio() != null) {
          registro.createCell(4).setCellValue(estudiante.promedio().doubleValue());
          registro.getCell(4).setCellStyle(formatoPromedio);
        } else {
          registro.createCell(4).setCellValue("");
        }
      }
      for (int columna = 0; columna < 5; columna++) {
        hoja.autoSizeColumn(columna);
      }
      libro.write(salida);
      return salida.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "No se pudo generar el Excel del cuadro de mérito", exception);
    }
  }

  private void agregarEncabezado(PdfPTable tabla, String texto) {
    PdfPCell celda = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 9, Font.BOLD)));
    celda.setBackgroundColor(COLOR_INSTITUCIONAL);
    celda.setPadding(6);
    tabla.addCell(celda);
  }

  private void agregarCelda(PdfPTable tabla, String texto) {
    PdfPCell celda = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 9)));
    celda.setPadding(5);
    tabla.addCell(celda);
  }

  private String valor(Object valor) {
    if (valor == null) {
      return "Sin puesto";
    }
    if (valor instanceof java.math.BigDecimal decimal) {
      return decimal.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString();
    }
    return valor.toString();
  }
}
