package pe.edu.pucp.colegio.common.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.GrayColor;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.AreaLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.CompetenciaLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.PeriodoLibreta;

@Service
public class LibretaPdfService {

  private static final ZoneId ZONA_ESCOLAR = ZoneId.of("America/Lima");
  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter FECHA_HORA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z");
  private static final String MARCA_BORRADOR = "BORRADOR - DOCUMENTO NO OFICIAL";
  private static final Color COLOR_INSTITUCIONAL = new Color(26, 72, 112);

  public byte[] generar(LibretaNotasResponse libreta, boolean preliminar) {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    Document documento = new Document(PageSize.A4, 38, 38, 42, 38);
    try {
      PdfWriter escritor = PdfWriter.getInstance(documento, salida);
      if (preliminar) {
        BaseFont fuenteMarca =
            BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        escritor.setPageEvent(new MarcaAguaBorrador(fuenteMarca));
      }
      documento.open();
      agregarEncabezado(documento, libreta, preliminar);
      for (PeriodoLibreta periodo : libreta.periodos()) {
        agregarPeriodo(documento, periodo);
      }
      documento.close();
      return salida.toByteArray();
    } catch (DocumentException | IOException exception) {
      if (documento.isOpen()) {
        documento.close();
      }
      throw new IllegalStateException("No se pudo generar el PDF de la libreta", exception);
    }
  }

  private void agregarEncabezado(
      Document documento, LibretaNotasResponse libreta, boolean preliminar)
      throws DocumentException {
    Font titulo =
        new Font(Font.HELVETICA, 17, Font.BOLD, preliminar ? Color.DARK_GRAY : COLOR_INSTITUCIONAL);
    Paragraph encabezado =
        new Paragraph(
            (preliminar ? "INFORME DE PROGRESO" : "LIBRETA OFICIAL DE NOTAS")
                + " - BIMESTRE "
                + libreta.bimestre()
                + (preliminar ? " - BORRADOR" : ""),
            titulo);
    encabezado.setAlignment(Element.ALIGN_CENTER);
    documento.add(encabezado);

    if (libreta.evaluacionesSinBimestre() > 0) {
      documento.add(
          new Paragraph(
              "Aviso: "
                  + libreta.evaluacionesSinBimestre()
                  + " evaluación(es) sin bimestre asignado no se incluyen en este informe.",
              new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY)));
    }

    Paragraph identificacion = new Paragraph();
    identificacion.add(new Chunk("Estudiante: ", new Font(Font.HELVETICA, 10, Font.BOLD)));
    identificacion.add(libreta.estudiante());
    identificacion.add(Chunk.NEWLINE);
    identificacion.add(new Chunk("Código: ", new Font(Font.HELVETICA, 10, Font.BOLD)));
    identificacion.add(String.valueOf(libreta.estudianteId()));
    identificacion.add(new Chunk("    Año escolar: ", new Font(Font.HELVETICA, 10, Font.BOLD)));
    identificacion.add(String.valueOf(libreta.anhioEscolar()));
    identificacion.setSpacingBefore(14);
    identificacion.setSpacingAfter(10);
    documento.add(identificacion);

    if (preliminar) {
      Paragraph aviso =
          new Paragraph(MARCA_BORRADOR, new Font(Font.HELVETICA, 11, Font.BOLD, Color.RED));
      aviso.setAlignment(Element.ALIGN_CENTER);
      documento.add(aviso);
      Paragraph fechaConsulta =
          new Paragraph(
              "Consulta realizada: " + ZonedDateTime.now(ZONA_ESCOLAR).format(FECHA_HORA),
              new Font(Font.HELVETICA, 9));
      fechaConsulta.setAlignment(Element.ALIGN_CENTER);
      fechaConsulta.setSpacingAfter(14);
      documento.add(fechaConsulta);
    } else {
      Paragraph aviso =
          new Paragraph(
              "Documento definitivo emitido después del cierre administrativo de actas.",
              new Font(Font.HELVETICA, 9));
      aviso.setAlignment(Element.ALIGN_CENTER);
      aviso.setSpacingAfter(14);
      documento.add(aviso);
    }
  }

  private void agregarPeriodo(Document documento, PeriodoLibreta periodo) throws DocumentException {
    Paragraph tituloPeriodo =
        new Paragraph(
            "Grado " + periodo.gradoNumero() + "° " + " - Sección " + periodo.seccion(),
            new Font(Font.HELVETICA, 12, Font.BOLD, COLOR_INSTITUCIONAL));
    tituloPeriodo.setSpacingBefore(10);
    tituloPeriodo.setSpacingAfter(4);
    documento.add(tituloPeriodo);
    documento.add(
        new Paragraph(
            "Del "
                + periodo.fechaInicio().format(FECHA)
                + " al "
                + periodo.fechaFin().format(FECHA),
            new Font(Font.HELVETICA, 9)));

    for (AreaLibreta area : periodo.areas()) {
      Paragraph tituloArea = new Paragraph(area.nombre(), new Font(Font.HELVETICA, 10, Font.BOLD));
      tituloArea.setSpacingBefore(7);
      tituloArea.setSpacingAfter(3);
      documento.add(tituloArea);

      PdfPTable tabla = new PdfPTable(new float[] {2.6f, 0.7f, 0.9f, 2.6f, 1.8f});
      tabla.setWidthPercentage(100);
      tabla.setHeaderRows(1);
      agregarCeldaEncabezado(tabla, "Competencia");
      agregarCeldaEncabezado(tabla, "Nota");
      agregarCeldaEncabezado(tabla, "Puntaje");
      agregarCeldaEncabezado(tabla, "Evaluaciones del bimestre");
      agregarCeldaEncabezado(tabla, "Observación");
      for (CompetenciaLibreta competencia : area.competencias()) {
        agregarCelda(tabla, competencia.nombre());
        agregarCelda(tabla, valor(competencia.nota()));
        agregarCelda(tabla, valor(competencia.puntajeConsolidado()));
        agregarCelda(tabla, detalleEvaluaciones(competencia));
        agregarCelda(tabla, valor(competencia.observacion()));
      }
      documento.add(tabla);
    }
  }

  private String detalleEvaluaciones(CompetenciaLibreta competencia) {
    if (competencia.evaluaciones().isEmpty()) {
      return "Sin evaluaciones asignadas";
    }

    return competencia.evaluaciones().stream()
        .map(
            evaluacion -> {
              String nota =
                  evaluacion.justificada()
                      ? "Justificada"
                      : evaluacion.nota() == null ? "Sin calificación" : evaluacion.nota();
              return evaluacion.titulo()
                  + ": "
                  + nota
                  + " ("
                  + evaluacion.ponderacion().stripTrailingZeros().toPlainString()
                  + "%)";
            })
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  private void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
    PdfPCell celda = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 8, Font.BOLD)));
    celda.setBackgroundColor(COLOR_INSTITUCIONAL);
    celda.setHorizontalAlignment(Element.ALIGN_LEFT);
    celda.setPadding(5);
    tabla.addCell(celda);
  }

  private void agregarCelda(PdfPTable tabla, String texto) {
    PdfPCell celda = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 8)));
    celda.setPadding(5);
    tabla.addCell(celda);
  }

  private String valor(Object valor) {
    return valor == null ? "-" : valor.toString();
  }

  private static class MarcaAguaBorrador extends PdfPageEventHelper {

    private final BaseFont fuente;

    private MarcaAguaBorrador(BaseFont fuente) {
      this.fuente = fuente;
    }

    @Override
    public void onEndPage(PdfWriter escritor, Document documento) {
      var lienzo = escritor.getDirectContentUnder();
      lienzo.saveState();
      lienzo.setColorFill(new GrayColor(0.88f));
      lienzo.beginText();
      lienzo.setFontAndSize(fuente, 34);
      lienzo.showTextAligned(
          Element.ALIGN_CENTER,
          MARCA_BORRADOR,
          (documento.left() + documento.right()) / 2,
          (documento.top() + documento.bottom()) / 2,
          45);
      lienzo.endText();
      lienzo.restoreState();
    }
  }
}
