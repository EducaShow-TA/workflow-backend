package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.config.AsistenciaProperties;

class VentanaMarcadoServiceTest {

  private static final ZoneId ZONA = ZoneId.of("America/Lima");
  private static final LocalDate FECHA = LocalDate.of(2026, 11, 4);
  private static final LocalTime INICIO = LocalTime.of(9, 0);

  @Test
  void abreLaVentanaExactamenteQuinceMinutosAntes() {
    assertDoesNotThrow(() -> serviceAt(FECHA.atTime(8, 45)).validar(FECHA, INICIO));
  }

  @Test
  void permiteMarcarAlInicioDeLaSesion() {
    assertDoesNotThrow(() -> serviceAt(FECHA.atTime(9, 0)).validar(FECHA, INICIO));
  }

  @Test
  void permiteMarcarEnElLimiteFinalDeLaVentana() {
    assertDoesNotThrow(() -> serviceAt(FECHA.atTime(9, 30)).validar(FECHA, INICIO));
  }

  @Test
  void rechazaMarcarAntesDeLaApertura() {
    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> serviceAt(FECHA.atTime(8, 44)).validar(FECHA, INICIO));

    assertEquals(409, error.getStatusCode().value());
  }

  @Test
  void rechazaMarcarDespuesDelCierre() {
    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> serviceAt(FECHA.atTime(9, 31)).validar(FECHA, INICIO));

    assertEquals(409, error.getStatusCode().value());
  }

  private VentanaMarcadoService serviceAt(LocalDateTime ahora) {
    AsistenciaProperties properties = new AsistenciaProperties();
    properties.setVentanaMarcadoAntesMinutos(15);
    properties.setVentanaMarcadoDespuesMinutos(30);
    Clock clock = Clock.fixed(ahora.atZone(ZONA).toInstant(), ZONA);
    return new VentanaMarcadoService(properties, clock);
  }
}
