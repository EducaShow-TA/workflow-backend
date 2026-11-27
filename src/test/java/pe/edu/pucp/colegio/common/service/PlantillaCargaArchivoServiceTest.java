package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class PlantillaCargaArchivoServiceTest {

  private final PlantillaCargaArchivoService servicio = new PlantillaCargaArchivoService();

  @Test
  void generarExcel_dejaPrimeraHojaListaParaImportarYAgregaUnaGuia() throws Exception {
    byte[] contenido =
        servicio.generarExcel(
            "Plantilla de notas",
            "Año 2026 · Bimestre 2",
            List.of("dni", "nombre", "nota"),
            List.of(List.of("12345678", "Ana Pérez", "")),
            List.of("Completa la nota con AD, A, B o C."));

    try (var libro = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
      var carga = libro.getSheetAt(0);
      assertEquals("Carga", carga.getSheetName());
      assertEquals("dni", carga.getRow(0).getCell(0).getStringCellValue());
      assertEquals("nota", carga.getRow(0).getCell(2).getStringCellValue());
      assertEquals("12345678", carga.getRow(1).getCell(0).getStringCellValue());
      assertEquals("", carga.getRow(1).getCell(2).getStringCellValue());
      assertTrue(carga.getRow(0).getCell(0).getCellStyle().getFillForegroundColor() > 0);
      assertEquals("Guía", libro.getSheetAt(1).getSheetName());
      assertEquals(
          "Plantilla de notas", libro.getSheetAt(1).getRow(0).getCell(0).getStringCellValue());
    }
  }
}
