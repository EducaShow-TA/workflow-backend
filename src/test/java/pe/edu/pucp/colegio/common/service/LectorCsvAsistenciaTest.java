package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.colegio.common.dto.FilaAsistenciaDocenteCsv;
import pe.edu.pucp.colegio.common.dto.FilaAsistenciaEstudianteCsv;
import pe.edu.pucp.colegio.common.exception.BusinessException;

class LectorCsvAsistenciaTest {

  private final LectorCsvAsistencia lector = new LectorCsvAsistencia();

  private static byte[] bytes(String texto) {
    return texto.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void leerDocentes_csvConColumnasOpcionales_retornaFilasConVaciosEnNulo() {
    String csv =
        "dni_docente,fecha,hora_entrada,hora_salida,estado,motivo\n"
            + "10555666,2026-11-18,07:55,15:30,,\n"
            + "10555668,2026-11-18,,,INASISTENCIA,Descanso\n";

    List<FilaAsistenciaDocenteCsv> filas = lector.leerDocentes(bytes(csv));

    assertEquals(2, filas.size());
    assertEquals(1, filas.get(0).fila());
    assertEquals("07:55", filas.get(0).horaEntrada());
    assertNull(filas.get(0).estado());
    assertEquals(2, filas.get(1).fila());
    assertNull(filas.get(1).horaEntrada());
    assertEquals("INASISTENCIA", filas.get(1).estado());
  }

  @Test
  void leerDocentes_puntoYComaConBomYMayusculas_retornaFilas() {
    String csv = "\uFEFFDNI_DOCENTE;Fecha\n10555666;18/11/2026\n";

    List<FilaAsistenciaDocenteCsv> filas = lector.leerDocentes(bytes(csv));

    assertEquals(1, filas.size());
    assertEquals("10555666", filas.get(0).dniDocente());
    assertEquals("18/11/2026", filas.get(0).fecha());
  }

  @Test
  void leerDocentes_sinColumnaFecha_informaColumnasFaltantes() {
    BusinessException e =
        assertThrows(
            BusinessException.class, () -> lector.leerDocentes(bytes("dni_docente\n10555666\n")));

    assertEquals("Faltan columnas obligatorias: fecha", e.getMessage());
  }

  @Test
  void leerAlumnos_ignoraColumnaNombre_retornaDniYEstado() {
    String csv = "dni_alumno,nombre,estado\n70333444,\"Torres Quispe, Luis\",PRESENTE\n";

    List<FilaAsistenciaEstudianteCsv> filas = lector.leerAlumnos(bytes(csv));

    assertEquals(1, filas.size());
    assertEquals("70333444", filas.get(0).dniAlumno());
    assertEquals("PRESENTE", filas.get(0).estado());
  }

  @Test
  void leerAlumnos_archivoSoloConCabecera_rechazaPorSinFilas() {
    BusinessException e =
        assertThrows(
            BusinessException.class, () -> lector.leerAlumnos(bytes("dni_alumno,estado\n")));

    assertEquals("El archivo no contiene filas de datos", e.getMessage());
  }

  @Test
  void leerAlumnos_masDeLasFilasPermitidas_rechaza() {
    StringBuilder csv = new StringBuilder("dni_alumno,estado\n");
    for (int i = 0; i <= LectorCsvAsistencia.MAX_FILAS; i++) {
      csv.append("70333444,PRESENTE\n");
    }

    BusinessException e =
        assertThrows(BusinessException.class, () -> lector.leerAlumnos(bytes(csv.toString())));

    assertEquals("El archivo supera el máximo de 500 filas", e.getMessage());
  }
}
