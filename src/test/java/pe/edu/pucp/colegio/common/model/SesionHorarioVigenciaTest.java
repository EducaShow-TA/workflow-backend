package pe.edu.pucp.colegio.common.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SesionHorarioVigenciaTest {

  @Test
  void vigenciaTemporal_incluyeAmbosExtremosYExcluyeLasFechasExternas() {
    SesionHorario sesion = new SesionHorario();
    sesion.setVigenteDesde(LocalDate.of(2026, 5, 1));
    sesion.setVigenteHasta(LocalDate.of(2026, 5, 31));

    assertFalse(sesion.estaVigenteEn(LocalDate.of(2026, 4, 30)));
    assertTrue(sesion.estaVigenteEn(LocalDate.of(2026, 5, 1)));
    assertTrue(sesion.estaVigenteEn(LocalDate.of(2026, 5, 31)));
    assertFalse(sesion.estaVigenteEn(LocalDate.of(2026, 6, 1)));
  }

  @Test
  void vigenciaPermanente_noTieneFechaFinal() {
    SesionHorario sesion = new SesionHorario();
    sesion.setVigenteDesde(LocalDate.of(2026, 5, 1));

    assertTrue(sesion.estaVigenteEn(LocalDate.of(2030, 1, 1)));
  }
}
