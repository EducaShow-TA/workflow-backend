package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.config.AsistenciaProperties;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteResponse;
import pe.edu.pucp.colegio.common.model.AsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AsistenciaDocenteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;

/** Marcaje de entrada y salida que el propio docente registra con la hora del servidor. */
@ExtendWith(MockitoExtension.class)
class AsistenciaDocenteMarcajeTest {

  private static final String USUARIO = "docente01";
  private static final LocalDate HOY = LocalDate.of(2026, 11, 4);
  private static final ZoneId ZONA = ZoneId.of("America/Lima");

  @Mock AsistenciaDocenteRepository asistencias;
  @Mock DocenteRepository docentes;
  @Mock SesionClaseRepository sesiones;
  @Mock DocenteService docenteService;

  private final AsistenciaProperties properties = new AsistenciaProperties();
  private Docente docente;

  @BeforeEach
  void preparar() {
    properties.setToleranciaDocenteMinutos(10);
    properties.setVentanaDocenteAntesMinutos(30);
    docente = new Docente();
    ReflectionTestUtils.setField(docente, "id", 1);
    docente.setPersona(new Persona("10000001", "Ana", "Docente", "Prueba"));
    Mockito.lenient().when(docenteService.obtenerDeCuenta(USUARIO)).thenReturn(docente);
    Mockito.lenient()
        .when(asistencias.saveAndFlush(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void marcarEntrada_dentroDeTolerancia_registraPresenteConHoraDelServidor() {
    prepararPrimeraSesion("08:00");
    sinAsistenciaHoy();

    AsistenciaDocenteResponse response = servicioALas("08:10").marcarEntrada(USUARIO);

    assertEquals(EstadoAsistenciaDocente.PRESENTE, response.estado());
    assertEquals(HOY, response.fecha());
    assertEquals(LocalTime.of(8, 10), response.horaEntrada());
    assertEquals(1, response.docenteId());
  }

  @Test
  void marcarEntrada_despuesDeTolerancia_registraTardanza() {
    prepararPrimeraSesion("08:00");
    sinAsistenciaHoy();

    AsistenciaDocenteResponse response = servicioALas("08:11").marcarEntrada(USUARIO);

    assertEquals(EstadoAsistenciaDocente.TARDANZA, response.estado());
    assertEquals(LocalTime.of(8, 11), response.horaEntrada());
    assertEquals("Marcaje posterior a la tolerancia de 10 minutos", response.motivoAjuste());
  }

  @Test
  void marcarEntrada_alAbrirLaVentana_registraPresente() {
    prepararPrimeraSesion("08:00");
    sinAsistenciaHoy();

    AsistenciaDocenteResponse response = servicioALas("07:30").marcarEntrada(USUARIO);

    assertEquals(EstadoAsistenciaDocente.PRESENTE, response.estado());
  }

  @Test
  void marcarEntrada_antesDeAbrirLaVentana_lanzaConflicto() {
    prepararPrimeraSesion("08:00");

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> servicioALas("07:29").marcarEntrada(USUARIO));

    assertEquals(409, error.getStatusCode().value());
    verify(asistencias, never()).saveAndFlush(any());
  }

  @Test
  void marcarEntrada_sinSesionesEseDia_registraPresente() {
    when(sesiones.findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(1, HOY))
        .thenReturn(Optional.empty());
    sinAsistenciaHoy();

    AsistenciaDocenteResponse response = servicioALas("15:40").marcarEntrada(USUARIO);

    assertEquals(EstadoAsistenciaDocente.PRESENTE, response.estado());
    assertEquals(LocalTime.of(15, 40), response.horaEntrada());
  }

  @Test
  void marcarEntrada_conEntradaYaMarcada_lanzaConflicto() {
    prepararPrimeraSesion("08:00");
    AsistenciaDocente existente = asistenciaDeHoy();
    existente.marcarIngreso(LocalTime.of(7, 50));
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY))
        .thenReturn(Optional.of(existente));

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> servicioALas("08:05").marcarEntrada(USUARIO));

    assertEquals(409, error.getStatusCode().value());
    assertEquals(LocalTime.of(7, 50), existente.getHoraEntrada());
  }

  @Test
  void marcarEntrada_conRegistroPendienteDelAdministrador_completaEseRegistro() {
    prepararPrimeraSesion("08:00");
    AsistenciaDocente pendiente = asistenciaDeHoy();
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY))
        .thenReturn(Optional.of(pendiente));

    AsistenciaDocenteResponse response = servicioALas("07:55").marcarEntrada(USUARIO);

    assertEquals(EstadoAsistenciaDocente.PRESENTE, response.estado());
    assertEquals(LocalTime.of(7, 55), pendiente.getHoraEntrada());
  }

  @Test
  void marcarEntrada_conInasistenciaRegistrada_lanzaConflicto() {
    prepararPrimeraSesion("08:00");
    AsistenciaDocente inasistencia = asistenciaDeHoy();
    inasistencia.marcarInasistencia();
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY))
        .thenReturn(Optional.of(inasistencia));

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> servicioALas("08:05").marcarEntrada(USUARIO));

    assertEquals(409, error.getStatusCode().value());
    assertEquals(EstadoAsistenciaDocente.INASISTENCIA, inasistencia.getEstado());
  }

  @Test
  void marcarEntrada_conMarcajeSimultaneo_lanzaConflicto() {
    prepararPrimeraSesion("08:00");
    sinAsistenciaHoy();
    when(asistencias.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("uk_asistencia_docente_fecha"));

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> servicioALas("08:05").marcarEntrada(USUARIO));

    assertEquals(409, error.getStatusCode().value());
  }

  @Test
  void marcarSalida_conEntradaMarcada_registraHoraDelServidor() {
    AsistenciaDocente asistencia = asistenciaDeHoy();
    asistencia.marcarIngreso(LocalTime.of(7, 55));
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY))
        .thenReturn(Optional.of(asistencia));

    AsistenciaDocenteResponse response = servicioALas("14:30").marcarSalida(USUARIO);

    assertEquals(LocalTime.of(14, 30), response.horaSalida());
    assertEquals(LocalTime.of(7, 55), response.horaEntrada());
    assertEquals(EstadoAsistenciaDocente.PRESENTE, response.estado());
  }

  @Test
  void marcarSalida_conTardanza_conservaElEstado() {
    AsistenciaDocente asistencia = asistenciaDeHoy();
    asistencia.marcarIngreso(LocalTime.of(8, 20));
    asistencia.ajustar(EstadoAsistenciaDocente.TARDANZA, "Marcaje posterior a la tolerancia");
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY))
        .thenReturn(Optional.of(asistencia));

    AsistenciaDocenteResponse response = servicioALas("14:30").marcarSalida(USUARIO);

    assertEquals(EstadoAsistenciaDocente.TARDANZA, response.estado());
    assertEquals(LocalTime.of(14, 30), response.horaSalida());
  }

  @Test
  void marcarSalida_sinRegistroDelDia_lanzaConflicto() {
    sinAsistenciaHoy();

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> servicioALas("14:30").marcarSalida(USUARIO));

    assertEquals(409, error.getStatusCode().value());
  }

  @Test
  void marcarSalida_conRegistroSinEntrada_lanzaConflicto() {
    AsistenciaDocente pendiente = asistenciaDeHoy();
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY))
        .thenReturn(Optional.of(pendiente));

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> servicioALas("14:30").marcarSalida(USUARIO));

    assertEquals(409, error.getStatusCode().value());
    assertNull(pendiente.getHoraSalida());
  }

  @Test
  void marcarSalida_conSalidaYaMarcada_lanzaConflicto() {
    AsistenciaDocente asistencia = asistenciaDeHoy();
    asistencia.marcarIngreso(LocalTime.of(7, 55));
    asistencia.marcarSalida(LocalTime.of(13, 0));
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY))
        .thenReturn(Optional.of(asistencia));

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> servicioALas("14:30").marcarSalida(USUARIO));

    assertEquals(409, error.getStatusCode().value());
    assertEquals(LocalTime.of(13, 0), asistencia.getHoraSalida());
  }

  @Test
  void listarPropias_conRango_consultaSoloAlDocenteAutenticado() {
    LocalDate desde = HOY.minusDays(7);
    AsistenciaDocente asistencia = asistenciaDeHoy();
    asistencia.marcarIngreso(LocalTime.of(7, 55));
    when(asistencias.buscarDelDocente(1, desde, HOY)).thenReturn(List.of(asistencia));

    List<AsistenciaDocenteResponse> response =
        servicioALas("09:00").listarPropias(USUARIO, desde, HOY);

    assertEquals(1, response.size());
    assertEquals(1, response.get(0).docenteId());
  }

  @Test
  void listarPropias_conRangoInvertido_lanzaBadRequest() {
    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> servicioALas("09:00").listarPropias(USUARIO, HOY, HOY.minusDays(1)));

    assertEquals(400, error.getStatusCode().value());
  }

  private AsistenciaDocenteService servicioALas(String hora) {
    Clock reloj =
        Clock.fixed(LocalDateTime.of(HOY, LocalTime.parse(hora)).atZone(ZONA).toInstant(), ZONA);
    return new AsistenciaDocenteService(
        asistencias, docentes, sesiones, properties, docenteService, reloj);
  }

  private void prepararPrimeraSesion(String horaInicio) {
    SesionClase sesion = new SesionClase();
    sesion.setHoraInicio(LocalTime.parse(horaInicio));
    when(sesiones.findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(1, HOY))
        .thenReturn(Optional.of(sesion));
  }

  private void sinAsistenciaHoy() {
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, HOY)).thenReturn(Optional.empty());
  }

  private AsistenciaDocente asistenciaDeHoy() {
    AsistenciaDocente asistencia = new AsistenciaDocente();
    asistencia.setDocente(docente);
    asistencia.setFecha(HOY);
    return asistencia;
  }
}
