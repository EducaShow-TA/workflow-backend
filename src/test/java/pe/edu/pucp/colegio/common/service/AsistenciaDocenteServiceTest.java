package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteCreateRequest;
import pe.edu.pucp.colegio.common.dto.ConfiguracionMarcajeDocenteResponse;
import pe.edu.pucp.colegio.common.model.AsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AsistenciaDocenteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;

@ExtendWith(MockitoExtension.class)
class AsistenciaDocenteServiceTest {

  private static final LocalDate FECHA = LocalDate.of(2026, 11, 4);

  @Mock AsistenciaDocenteRepository asistencias;
  @Mock DocenteRepository docentes;
  @Mock SesionClaseRepository sesiones;
  @Mock ConfiguracionMarcajeDocenteService configuracionMarcaje;

  @InjectMocks AsistenciaDocenteService service;

  private Docente docente;

  @BeforeEach
  void preparar() {
    docente = new Docente();
    ReflectionTestUtils.setField(docente, "id", 1);
    docente.setPersona(new Persona("10000001", "Ana", "Docente", "Prueba"));
    Mockito.lenient().when(docentes.findById(1)).thenReturn(Optional.of(docente));
    Mockito.lenient()
        .when(configuracionMarcaje.obtener())
        .thenReturn(new ConfiguracionMarcajeDocenteResponse(15, 30, 30));
  }

  @Test
  void registraIngresoPuntualDeUnDocente() {
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(1, FECHA)).thenReturn(false);
    when(sesiones.findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(1, FECHA))
        .thenReturn(Optional.empty());
    when(asistencias.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.registrar(registro(EstadoAsistenciaDocente.PRESENTE, "08:00", null));

    assertEquals(EstadoAsistenciaDocente.PRESENTE, response.estado());
    assertEquals(LocalTime.of(8, 0), response.horaEntrada());
    verify(asistencias).saveAndFlush(any(AsistenciaDocente.class));
  }

  @Test
  void registraTardanzaConLaToleranciaGlobalConfigurada() {
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(1, FECHA)).thenReturn(false);
    SesionClase sesion = new SesionClase();
    sesion.setHoraInicio(LocalTime.of(8, 0));
    when(sesiones.findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(1, FECHA))
        .thenReturn(Optional.of(sesion));
    when(asistencias.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.registrar(registro(EstadoAsistenciaDocente.PRESENTE, "08:16", null));

    assertEquals(EstadoAsistenciaDocente.TARDANZA, response.estado());
    assertEquals("Registro manual de asistencia", response.motivoAjuste());
  }

  @Test
  void impideDosRegistrosDelMismoDocenteYFecha() {
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(1, FECHA)).thenReturn(true);

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(registro(EstadoAsistenciaDocente.PRESENTE, "08:00", null)));

    assertEquals(409, error.getStatusCode().value());
    verify(asistencias, never()).saveAndFlush(any());
  }

  @Test
  void devuelveNotFoundCuandoElDocenteNoExiste() {
    when(docentes.findById(1)).thenReturn(Optional.empty());

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(registro(EstadoAsistenciaDocente.PRESENTE, "08:00", null)));

    assertEquals(404, error.getStatusCode().value());
  }

  @Test
  void rechazaSalidaAnteriorAlIngreso() {
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(1, FECHA)).thenReturn(false);

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.registrar(registro(EstadoAsistenciaDocente.PRESENTE, "09:00", "08:59")));

    assertEquals(400, error.getStatusCode().value());
    verify(asistencias, never()).saveAndFlush(any());
  }

  @Test
  void ajustaEstadoDeCualquierDocenteConMotivo() {
    AsistenciaDocente asistencia = new AsistenciaDocente();
    asistencia.setDocente(docente);
    asistencia.setFecha(FECHA);
    asistencia.marcarIngreso(LocalTime.of(8, 20));
    when(asistencias.findByIdAndActiveTrue(5)).thenReturn(Optional.of(asistencia));

    var response =
        service.ajustar(
            5,
            new AsistenciaDocenteAjusteRequest(
                EstadoAsistenciaDocente.TARDANZA, "Revisión de marcaje"));

    assertEquals(EstadoAsistenciaDocente.TARDANZA, response.estado());
    assertEquals("Revisión de marcaje", response.motivoAjuste());
  }

  @Test
  void llegadaEnElUltimoMinutoDeToleranciaSeMantienePresente() {
    prepararSesion("08:00");
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(1, FECHA)).thenReturn(false);
    when(asistencias.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.registrar(registro(EstadoAsistenciaDocente.PRESENTE, "08:15", null));

    assertEquals(EstadoAsistenciaDocente.PRESENTE, response.estado());
  }

  @Test
  void llegadaPosteriorALaToleranciaGlobalSeClasificaComoTardanza() {
    prepararSesion("08:00");
    when(asistencias.existsByDocenteIdAndFechaAndActiveTrue(1, FECHA)).thenReturn(false);
    when(asistencias.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.registrar(registro(EstadoAsistenciaDocente.PRESENTE, "08:16", null));

    assertEquals(EstadoAsistenciaDocente.TARDANZA, response.estado());
    assertEquals("Registro manual de asistencia", response.motivoAjuste());
  }

  @Test
  void venceLaToleranciaYRegistraInasistenciaAutomatica() {
    ReflectionTestUtils.setField(
        service,
        "clock",
        Clock.fixed(
            FECHA.atTime(8, 31).atZone(ZoneId.of("America/Lima")).toInstant(),
            ZoneId.of("America/Lima")));
    SesionClase sesion = new SesionClase();
    sesion.setDocente(docente);
    sesion.setHoraInicio(LocalTime.of(8, 0));
    when(sesiones.findAllByFechaAndActiveTrueAndCanceladaFalseOrderByHoraInicioAsc(FECHA))
        .thenReturn(List.of(sesion));
    when(asistencias.findByDocenteIdAndFechaAndActiveTrue(1, FECHA)).thenReturn(Optional.empty());
    when(asistencias.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.procesarInasistenciasPendientes();

    var captor = org.mockito.ArgumentCaptor.forClass(AsistenciaDocente.class);
    verify(asistencias).saveAndFlush(captor.capture());
    assertEquals(EstadoAsistenciaDocente.INASISTENCIA, captor.getValue().getEstado());
    assertEquals(
        "Inasistencia automática: venció la ventana de marcaje",
        captor.getValue().getMotivoAjuste());
  }

  private void prepararSesion(String horaInicio) {
    SesionClase sesion = new SesionClase();
    sesion.setHoraInicio(LocalTime.parse(horaInicio));
    when(sesiones.findFirstByDocenteIdAndFechaAndActiveTrueOrderByHoraInicioAsc(1, FECHA))
        .thenReturn(Optional.of(sesion));
  }

  private AsistenciaDocenteCreateRequest registro(
      EstadoAsistenciaDocente estado, String entrada, String salida) {
    return new AsistenciaDocenteCreateRequest(
        1,
        FECHA,
        estado,
        entrada == null ? null : LocalTime.parse(entrada),
        salida == null ? null : LocalTime.parse(salida),
        "Registro manual de asistencia");
  }
}
