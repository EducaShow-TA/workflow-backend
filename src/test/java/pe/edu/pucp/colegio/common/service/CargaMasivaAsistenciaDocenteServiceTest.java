package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteCreateRequest;
import pe.edu.pucp.colegio.common.dto.CargaMasivaAsistenciaResultadoDTO;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.AsistenciaDocenteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;

@ExtendWith(MockitoExtension.class)
class CargaMasivaAsistenciaDocenteServiceTest {

  private static final ZoneId ZONA = ZoneId.of("America/Lima");
  private static final String CABECERA =
      "dni_docente,fecha,hora_entrada,hora_salida,estado,motivo\n";

  @Mock DocenteRepository docentes;
  @Mock AsistenciaDocenteRepository asistencias;
  @Mock AsistenciaDocenteService asistenciaService;

  private CargaMasivaAsistenciaDocenteService service;

  @BeforeEach
  void preparar() {
    Clock reloj = Clock.fixed(LocalDateTime.of(2026, 11, 19, 10, 0).atZone(ZONA).toInstant(), ZONA);
    service =
        new CargaMasivaAsistenciaDocenteService(
            new LectorCsvAsistencia(), docentes, asistencias, asistenciaService, reloj);
  }

  private void docenteExistente(String dni, int id) {
    Docente docente = new Docente();
    ReflectionTestUtils.setField(docente, "id", id);
    docente.setPersona(new Persona(dni, "Ana", "Docente", "Prueba"));
    when(docentes.findByPersonaDniAndActiveTrue(dni)).thenReturn(Optional.of(docente));
  }

  private CargaMasivaAsistenciaResultadoDTO cargar(String csv, boolean soloValidar) {
    return service.cargar((CABECERA + csv).getBytes(StandardCharsets.UTF_8), soloValidar, "admin");
  }

  @Test
  void cargar_filasValidas_guardaElLoteYDistingueNuevosDeActualizados() {
    docenteExistente("10555666", 1);
    docenteExistente("10555667", 2);
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(1, LocalDate.of(2026, 11, 18)))
        .thenReturn(false);
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(2, LocalDate.of(2026, 11, 18)))
        .thenReturn(true);

    var resultado =
        cargar(
            "10555666,2026-11-18,07:55,15:30,,\n10555667,18/11/2026,,,INASISTENCIA,Descanso\n",
            false);

    assertTrue(resultado.guardado());
    assertEquals(2, resultado.filasValidas());
    assertEquals(1, resultado.registrosNuevos());
    assertEquals(1, resultado.registrosActualizados());
    ArgumentCaptor<List<AsistenciaDocenteCreateRequest>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(asistenciaService).registrarOActualizarLote(captor.capture());
    assertEquals(EstadoAsistenciaDocente.PRESENTE, captor.getValue().get(0).estado());
    assertEquals(LocalTime.of(7, 55), captor.getValue().get(0).horaEntrada());
    assertEquals(EstadoAsistenciaDocente.INASISTENCIA, captor.getValue().get(1).estado());
  }

  @Test
  void cargar_soloValidar_noGuardaAunqueNoHayaErrores() {
    docenteExistente("10555666", 1);

    var resultado = cargar("10555666,2026-11-18,07:55,15:30,,\n", true);

    assertFalse(resultado.guardado());
    assertTrue(resultado.soloValidacion());
    assertEquals(1, resultado.filasValidas());
    assertTrue(resultado.errores().isEmpty());
    verify(asistenciaService, never()).registrarOActualizarLote(anyList());
  }

  @Test
  void cargar_conUnaFilaInvalida_informaErroresPorFilaYNoGuardaNinguna() {
    docenteExistente("10555666", 1);
    when(docentes.findByPersonaDniAndActiveTrue("10999999")).thenReturn(Optional.empty());

    var resultado =
        cargar(
            "10555666,2026-11-18,07:55,15:30,,\n"
                + "10999999,2026-11-18,07:55,,,\n"
                + "10555666,2026-12-31,07:55,,,\n"
                + "10555666,2026-11-17,15:30,07:55,,\n"
                + "10555666,2026-11-16,,,TARDANZA,\n",
            false);

    assertFalse(resultado.guardado());
    assertEquals(5, resultado.totalFilas());
    assertEquals(4, resultado.filasRechazadas());
    assertTrue(
        resultado.errores().stream()
            .anyMatch(e -> e.fila() == 2 && e.campo().equals("dni_docente")));
    assertTrue(
        resultado.errores().stream().anyMatch(e -> e.fila() == 3 && e.campo().equals("fecha")));
    assertTrue(
        resultado.errores().stream()
            .anyMatch(e -> e.fila() == 4 && e.campo().equals("hora_salida")));
    assertTrue(
        resultado.errores().stream().anyMatch(e -> e.fila() == 5 && e.campo().equals("motivo")));
    verify(asistenciaService, never()).registrarOActualizarLote(anyList());
  }

  @Test
  void cargar_mismoDocenteYFechaDosVeces_rechazaLaSegundaFila() {
    docenteExistente("10555666", 1);

    var resultado = cargar("10555666,2026-11-18,07:55,,,\n10555666,2026-11-18,08:00,,,\n", false);

    assertFalse(resultado.guardado());
    assertEquals(1, resultado.filasRechazadas());
    assertEquals(2, resultado.errores().get(0).fila());
    verify(asistenciaService, never()).registrarOActualizarLote(any());
  }

  @Test
  void cargar_estadoNoPermitido_informaLosEstadosValidos() {
    docenteExistente("10555666", 1);

    var resultado = cargar("10555666,2026-11-18,07:55,,PENDIENTE_MARCAJE,\n", false);

    assertEquals(1, resultado.errores().size());
    assertEquals("estado", resultado.errores().get(0).campo());
    verify(asistenciaService, never()).registrarOActualizarLote(anyList());
  }
}
