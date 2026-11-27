package pe.edu.pucp.colegio.common.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import pe.edu.pucp.colegio.common.model.Matricula;

@Service
public class ConstanciaMatriculaPdfService {

  private static final Color COLOR_INSTITUCIONAL = new Color(26, 72, 112);
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public byte[] generar(Matricula matricula) {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    Document documento = new Document(PageSize.A4, 54, 54, 54, 54);
    try {
      PdfWriter.getInstance(documento, salida);
      documento.open();

      Paragraph institucion =
          new Paragraph(
              "EducaShow · Colegio", new Font(Font.HELVETICA, 15, Font.BOLD, COLOR_INSTITUCIONAL));
      institucion.setAlignment(Paragraph.ALIGN_CENTER);
      documento.add(institucion);

      Paragraph titulo =
          new Paragraph(
              "CONSTANCIA DE MATRÍCULA", new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK));
      titulo.setAlignment(Paragraph.ALIGN_CENTER);
      titulo.setSpacingBefore(28);
      titulo.setSpacingAfter(24);
      documento.add(titulo);

      var persona = matricula.getEstudiante().getPersona();
      var periodo = matricula.getPeriodo();
      documento.add(
          new Paragraph(
              "Se deja constancia de que "
                  + persona.getNombreCompleto()
                  + ", identificado(a) con "
                  + persona.getTipoDocumento()
                  + " "
                  + persona.getDni()
                  + ", está matriculado(a) para el año escolar "
                  + periodo.getFechaInicio().getYear()
                  + " en "
                  + NombreSeccion.completa(
                      periodo.getGrado().getNumero(),
                      periodo.getSeccion().getCodigo(),
                      periodo.getGrado().getNivelEducativo())
                  + ".",
              new Font(Font.HELVETICA, 12)));

      Paragraph registro =
          new Paragraph(
              "Matrícula N.° "
                  + matricula.getId()
                  + " · Registrada el "
                  + matricula.getFechaMatricula().format(FECHA),
              new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY));
      registro.setSpacingBefore(18);
      documento.add(registro);

      Paragraph pie =
          new Paragraph(
              "Documento generado por el sistema EducaShow.",
              new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY));
      pie.setSpacingBefore(40);
      documento.add(pie);

      documento.close();
      return salida.toByteArray();
    } catch (DocumentException exception) {
      if (documento.isOpen()) {
        documento.close();
      }
      throw new IllegalStateException("No se pudo generar la constancia de matrícula", exception);
    }
  }
}
