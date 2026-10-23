package pe.edu.pucp.colegio.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;

class LectorCsvUsuariosTest {

  private static final String CABECERA =
      "rol,dni,nombres,apellido_paterno,apellido_materno,correo,telefono,fecha_nacimiento,"
          + "dni_apoderado,parentesco";

  private final LectorCsvUsuarios lector = new LectorCsvUsuarios();

  private static byte[] bytes(String texto) {
    return texto.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void leer_csvConComas_retornaFilasConCamposVaciosEnNulo() {
    String csv =
        CABECERA
            + "\nALUMNO,70333444,Luis,Torres,Quispe,luis@colegio.edu.pe,,2011-05-14,40111222,MADRE"
            + "\nDOCENTE,10555666,Carlos,Vega,Rios,carlos@colegio.edu.pe,,,,\n";

    List<FilaUsuarioCsv> filas = lector.leer(bytes(csv));

    assertEquals(2, filas.size());
    assertEquals(1, filas.get(0).fila());
    assertEquals("ALUMNO", filas.get(0).rol());
    assertEquals("40111222", filas.get(0).dniApoderado());
    assertEquals(2, filas.get(1).fila());
    assertNull(filas.get(1).telefono());
    assertNull(filas.get(1).dniApoderado());
  }

  @Test
  void leer_csvConPuntoYComaYBom_retornaFilas() {
    String csv =
        "\uFEFFROL;DNI;Nombres;apellido_paterno;apellido_materno;correo\n"
            + "DOCENTE;10555666;Carlos;Vega;Rios;carlos@colegio.edu.pe\n";

    List<FilaUsuarioCsv> filas = lector.leer(bytes(csv));

    assertEquals(1, filas.size());
    assertEquals("DOCENTE", filas.get(0).rol());
    assertEquals("Carlos", filas.get(0).nombres());
  }

  @Test
  void leer_sinColumnaObligatoria_lanzaBusinessExceptionConLasFaltantes() {
    String csv = "rol,dni,nombres\nDOCENTE,10555666,Carlos\n";

    BusinessException e = assertThrows(BusinessException.class, () -> lector.leer(bytes(csv)));

    assertTrue(e.getMessage().contains("correo"));
    assertTrue(e.getMessage().contains("apellido_paterno"));
  }

  @Test
  void leer_archivoVacio_lanzaBusinessException() {
    assertThrows(BusinessException.class, () -> lector.leer(bytes("   \n")));
  }

  @Test
  void leer_soloCabecera_lanzaBusinessException() {
    assertThrows(BusinessException.class, () -> lector.leer(bytes(CABECERA + "\n")));
  }

  @Test
  void leer_masFilasDelMaximo_lanzaBusinessException() {
    StringBuilder csv = new StringBuilder(CABECERA).append('\n');
    for (int i = 0; i <= LectorCsvUsuarios.MAX_FILAS; i++) {
      csv.append("DOCENTE,").append(i).append(",A,B,C,u").append(i).append("@x.pe,,,,\n");
    }

    assertThrows(BusinessException.class, () -> lector.leer(bytes(csv.toString())));
  }
}
