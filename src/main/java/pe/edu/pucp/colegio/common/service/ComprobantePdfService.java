package pe.edu.pucp.colegio.common.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.stereotype.Service;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.ComprobantePago;
import pe.edu.pucp.colegio.common.model.MedioPago;

/** Comprobante de pago de una pensión en PDF. No reemplaza a un comprobante electrónico. */
@Service
public class ComprobantePdfService {

  private static final Color COLOR_INSTITUCIONAL = new Color(26, 72, 112);
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final Locale PERU = Locale.forLanguageTag("es-PE");

  public byte[] generar(ComprobantePago comprobante) {
    BoletaPension boleta = comprobante.getBoleta();
    var compromiso = boleta.getCompromiso();
    var matricula = compromiso.getMatricula();
    var periodo = matricula.getPeriodo();

    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    Document documento = new Document(PageSize.A5.rotate(), 36, 36, 32, 32);
    try {
      PdfWriter.getInstance(documento, salida);
      documento.open();

      PdfPTable cabecera = new PdfPTable(new float[] {3, 2});
      cabecera.setWidthPercentage(100);
      PdfPCell institucion = celda("");
      institucion.addElement(
          new Paragraph(
              "EducaShow · Colegio", new Font(Font.HELVETICA, 15, Font.BOLD, COLOR_INSTITUCIONAL)));
      institucion.addElement(
          new Paragraph(
              "Comprobante de pago de pensión",
              new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY)));
      cabecera.addCell(institucion);
      PdfPCell numero = celda("");
      Paragraph codigo =
          new Paragraph(
              comprobante.getNumeroComprobante(), new Font(Font.HELVETICA, 12, Font.BOLD));
      codigo.setAlignment(Element.ALIGN_RIGHT);
      Paragraph emision =
          new Paragraph(
              "Emitido el " + comprobante.getFechaEmision().format(FECHA),
              new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY));
      emision.setAlignment(Element.ALIGN_RIGHT);
      numero.addElement(codigo);
      numero.addElement(emision);
      cabecera.addCell(numero);
      documento.add(cabecera);

      PdfPTable linea = new PdfPTable(1);
      linea.setWidthPercentage(100);
      linea.setSpacingBefore(6);
      linea.setSpacingAfter(12);
      PdfPCell borde = celda("");
      borde.setBorder(Rectangle.BOTTOM);
      borde.setBorderColor(COLOR_INSTITUCIONAL);
      borde.setBorderWidthBottom(1.5f);
      linea.addCell(borde);
      documento.add(linea);

      String mes =
          boleta.getMesCobro().getMonth().getDisplayName(TextStyle.FULL, PERU)
              + " "
              + boleta.getMesCobro().getYear();
      PdfPTable datos = new PdfPTable(new float[] {2, 5});
      datos.setWidthPercentage(100);
      fila(datos, "Estudiante", matricula.getEstudiante().getPersona().getNombreCompleto());
      fila(
          datos,
          "Grado y sección",
          NombreSeccion.completa(
              periodo.getGrado().getNumero(),
              periodo.getSeccion().getCodigo(),
              periodo.getGrado().getNivelEducativo()));
      fila(datos, "Apoderado titular", compromiso.getApoderado().getPersona().getNombreCompleto());
      fila(datos, "Concepto", compromiso.getConcepto() + " · " + mes);
      fila(
          datos,
          "Medio de pago",
          comprobante.getMedioPago() == MedioPago.VIRTUAL ? "Virtual" : "Presencial");
      documento.add(datos);

      PdfPTable total = new PdfPTable(new float[] {1, 1});
      total.setWidthPercentage(100);
      total.setSpacingBefore(12);
      Font negrita = new Font(Font.HELVETICA, 13, Font.BOLD);
      PdfPCell etiqueta = new PdfPCell(new Phrase("Monto pagado", negrita));
      etiqueta.setBorder(Rectangle.TOP);
      etiqueta.setPaddingTop(8);
      PdfPCell monto = new PdfPCell(new Phrase(soles(comprobante.getMontoPagado()), negrita));
      monto.setBorder(Rectangle.TOP);
      monto.setPaddingTop(8);
      monto.setHorizontalAlignment(Element.ALIGN_RIGHT);
      total.addCell(etiqueta);
      total.addCell(monto);
      documento.add(total);

      Paragraph pie =
          new Paragraph(
              "Documento generado por el sistema EducaShow. No es un comprobante electrónico de"
                  + " SUNAT.",
              new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY));
      pie.setSpacingBefore(18);
      documento.add(pie);

      documento.close();
      return salida.toByteArray();
    } catch (DocumentException exception) {
      if (documento.isOpen()) {
        documento.close();
      }
      throw new IllegalStateException("No se pudo generar el PDF del comprobante", exception);
    }
  }

  private void fila(PdfPTable tabla, String etiqueta, String valor) {
    PdfPCell izquierda = celda(etiqueta);
    izquierda.getPhrase().getFont().setColor(Color.GRAY);
    tabla.addCell(izquierda);
    PdfPCell derecha = new PdfPCell(new Phrase(valor, new Font(Font.HELVETICA, 10, Font.BOLD)));
    derecha.setBorder(Rectangle.NO_BORDER);
    derecha.setPaddingBottom(5);
    tabla.addCell(derecha);
  }

  private PdfPCell celda(String texto) {
    PdfPCell celda = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 10)));
    celda.setBorder(Rectangle.NO_BORDER);
    celda.setPaddingBottom(5);
    return celda;
  }

  private String soles(BigDecimal monto) {
    NumberFormat formato = NumberFormat.getNumberInstance(Locale.US);
    formato.setMinimumFractionDigits(2);
    formato.setMaximumFractionDigits(2);
    return "S/ " + formato.format(monto);
  }
}
