package pe.edu.pucp.colegio.common.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import pe.edu.pucp.colegio.common.dto.FilaReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.ReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.TotalesAsistenciaResponse;

/** Convierte el reporte de asistencia ya autorizado en un CSV UTF-8 legible por Excel. */
@Component
public class ReporteAsistenciaCsv {

  /** Excel solo reconoce UTF-8 (tildes y ñ) si el archivo empieza con la marca BOM. */
  private static final char BOM = '﻿';

  private static final String[] ENCABEZADO = {
    "Alumno",
    "Grado",
    "Seccion",
    "Presentes",
    "Tardanzas",
    "Inasistencias",
    "Justificadas",
    "Total sesiones",
    "% inasistencia"
  };

  public byte[] exportar(ReporteAsistenciaResponse reporte) {
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    try (Writer escritor = new OutputStreamWriter(salida, StandardCharsets.UTF_8);
        CSVPrinter csv = new CSVPrinter(escritor, CSVFormat.DEFAULT)) {
      escritor.write(BOM);
      csv.printRecord((Object[]) ENCABEZADO);
      for (FilaReporteAsistenciaResponse fila : reporte.alumnos()) {
        csv.printRecord(
            celdaTexto(fila.nombreCompleto()),
            celdaTexto(fila.grado()),
            celdaTexto(fila.seccion()),
            fila.presentes(),
            fila.tardanzas(),
            fila.inasistencias(),
            fila.justificadas(),
            fila.totalSesiones(),
            fila.porcentajeInasistencia().toPlainString());
      }
      TotalesAsistenciaResponse totales = reporte.totales();
      csv.printRecord(
          "TOTAL",
          "",
          "",
          totales.presentes(),
          totales.tardanzas(),
          totales.inasistencias(),
          totales.justificadas(),
          totales.totalSesiones(),
          ReporteAsistenciaService.porcentaje(totales.inasistencias(), totales.totalSesiones())
              .toPlainString());
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo generar el CSV del reporte de asistencia", e);
    }
    return salida.toByteArray();
  }

  /**
   * Excel ejecuta como fórmula una celda que empieza con =, +, - o @ (inyección CSV). Los nombres
   * pueden venir de la carga masiva, así que se anteponen con un apóstrofo para que sean texto.
   */
  private static String celdaTexto(String valor) {
    if (valor == null || valor.isEmpty()) {
      return valor;
    }
    char primero = valor.charAt(0);
    boolean peligroso =
        primero == '='
            || primero == '+'
            || primero == '-'
            || primero == '@'
            || primero == '\t'
            || primero == '\r';
    return peligroso ? "'" + valor : valor;
  }
}
