package pe.edu.pucp.colegio.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.colegio.common.dto.FilaReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.ReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.TotalesAsistenciaResponse;

class ReporteAsistenciaCsvTest {

  private static final String ENCABEZADO =
      "Alumno,Grado,Seccion,Presentes,Tardanzas,Inasistencias,Justificadas,Total sesiones,"
          + "% inasistencia";

  private final ReporteAsistenciaCsv csv = new ReporteAsistenciaCsv();

  @Test
  void exportar_reporte_incluyeBomEncabezadoFilasYTotal() {
    byte[] bytes =
        csv.exportar(
            reporte(
                List.of(fila("Ana Quispe Rojas", 40, 5, 6, 1, "11.5")),
                new TotalesAsistenciaResponse(40, 5, 6, 1, 52)));

    assertThat(bytes[0]).isEqualTo((byte) 0xEF);
    assertThat(bytes[1]).isEqualTo((byte) 0xBB);
    assertThat(bytes[2]).isEqualTo((byte) 0xBF);
    String[] lineas = texto(bytes).split("\r\n");
    assertThat(lineas)
        .containsExactly(
            ENCABEZADO,
            "Ana Quispe Rojas,3.° Secundaria,A,40,5,6,1,52,11.5",
            "TOTAL,,,40,5,6,1,52,11.5");
  }

  @Test
  void exportar_nombreConComaYEnie_loEscapaSinPerderCaracteres() {
    byte[] bytes =
        csv.exportar(
            reporte(
                List.of(fila("Muñoz, Pedro", 1, 0, 0, 0, "0.0")),
                new TotalesAsistenciaResponse(1, 0, 0, 0, 1)));

    assertThat(texto(bytes)).contains("\"Muñoz, Pedro\",3.° Secundaria,A,1,0,0,0,1,0.0");
  }

  @Test
  void exportar_nombreQueEmpiezaConFormula_loNeutraliza() {
    byte[] bytes =
        csv.exportar(
            reporte(
                List.of(
                    fila("=HYPERLINK(\"http://x\")", 1, 0, 0, 0, "0.0"),
                    fila("+51 Pedro", 1, 0, 0, 0, "0.0"),
                    fila("-Ana", 1, 0, 0, 0, "0.0"),
                    fila("@Luis", 1, 0, 0, 0, "0.0")),
                new TotalesAsistenciaResponse(4, 0, 0, 0, 4)));

    String[] lineas = texto(bytes).split("\r\n");
    assertThat(lineas[1]).startsWith("\"'=HYPERLINK(");
    assertThat(lineas[2]).startsWith("'+51 Pedro,");
    assertThat(lineas[3]).startsWith("'-Ana,");
    assertThat(lineas[4]).startsWith("'@Luis,");
  }

  @Test
  void exportar_sinAlumnos_soloEncabezadoYTotalEnCero() {
    byte[] bytes = csv.exportar(reporte(List.of(), new TotalesAsistenciaResponse(0, 0, 0, 0, 0)));

    assertThat(texto(bytes).split("\r\n")).containsExactly(ENCABEZADO, "TOTAL,,,0,0,0,0,0,0.0");
  }

  private static String texto(byte[] bytes) {
    return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
  }

  private static ReporteAsistenciaResponse reporte(
      List<FilaReporteAsistenciaResponse> filas, TotalesAsistenciaResponse totales) {
    return new ReporteAsistenciaResponse(
        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 17), totales, filas);
  }

  private static FilaReporteAsistenciaResponse fila(
      String nombre, long p, long t, long i, long j, String porcentaje) {
    return new FilaReporteAsistenciaResponse(
        1,
        nombre,
        3,
        "3.° Secundaria",
        7,
        "A",
        p,
        t,
        i,
        j,
        p + t + i + j,
        new BigDecimal(porcentaje));
  }
}
