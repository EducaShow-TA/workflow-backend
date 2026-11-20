package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.colegio.common.dto.CargaMasivaAsistenciaResultadoDTO;
import pe.edu.pucp.colegio.common.dto.FilaListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.ListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.RegistroAsistenciaSesionRequest;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;

@ExtendWith(MockitoExtension.class)
class CargaMasivaAsistenciaEstudianteServiceTest {

  private static final SesionClaseService.Acceso ACCESO =
      new SesionClaseService.Acceso("docente", false);

  @Mock SesionClaseService sesiones;

  private CargaMasivaAsistenciaEstudianteService service;

  @BeforeEach
  void preparar() {
    service = new CargaMasivaAsistenciaEstudianteService(new LectorCsvAsistencia(), sesiones);
    when(sesiones.obtenerListaParaCarga(10, ACCESO))
        .thenReturn(
            new ListaAsistenciaResponse(
                null,
                null,
                null,
                null,
                List.of(
                    alumno(1, "70000001", null, true),
                    alumno(2, "70000002", EstadoAsistencia.PRESENTE, true),
                    alumno(3, "70000003", EstadoAsistencia.JUSTIFICADA, true),
                    alumno(4, "70000004", null, false))));
  }

  private static FilaListaAsistenciaResponse alumno(
      int id, String dni, EstadoAsistencia estado, boolean matriculado) {
    return new FilaListaAsistenciaResponse(id, dni, "Alumno " + id, null, estado, matriculado);
  }

  private CargaMasivaAsistenciaResultadoDTO cargar(String filas, boolean soloValidar) {
    return service.cargar(
        10, ("dni_alumno,estado\n" + filas).getBytes(StandardCharsets.UTF_8), soloValidar, ACCESO);
  }

  @Test
  void cargar_filasValidas_guardaConLasMismasReglasDeLaTomaManual() {
    var resultado = cargar("70000001,presente\n70000002,AUSENTE\n", false);

    assertTrue(resultado.guardado());
    assertEquals(1, resultado.registrosNuevos());
    assertEquals(1, resultado.registrosActualizados());
    ArgumentCaptor<RegistroAsistenciaSesionRequest> captor =
        ArgumentCaptor.forClass(RegistroAsistenciaSesionRequest.class);
    verify(sesiones).guardarAsistencias(eq(10), captor.capture(), any());
    assertEquals(2, captor.getValue().asistencias().size());
    assertEquals(1, captor.getValue().asistencias().get(0).estudianteId());
    assertEquals(EstadoAsistencia.PRESENTE, captor.getValue().asistencias().get(0).estado());
  }

  @Test
  void cargar_soloValidar_noGuarda() {
    var resultado = cargar("70000001,PRESENTE\n", true);

    assertFalse(resultado.guardado());
    assertEquals(1, resultado.filasValidas());
    verify(sesiones, never()).guardarAsistencias(any(), any(), any());
  }

  @Test
  void cargar_conErrores_informaFilaPorFilaYNoGuarda() {
    var resultado =
        cargar(
            "70000001,PRESENTE\n"
                + "70000004,PRESENTE\n"
                + "70000009,PRESENTE\n"
                + "7000,PRESENTE\n"
                + "70000001,TARDANZA\n"
                + "70000003,AUSENTE\n"
                + "70000002,JUSTIFICADA\n",
            false);

    assertFalse(resultado.guardado());
    assertEquals(7, resultado.totalFilas());
    assertEquals(6, resultado.filasRechazadas());
    assertEquals(
        List.of(2, 3, 4, 5, 6, 7),
        resultado.errores().stream().map(e -> e.fila()).distinct().toList());
    verify(sesiones, never()).guardarAsistencias(any(), any(), any());
  }
}
