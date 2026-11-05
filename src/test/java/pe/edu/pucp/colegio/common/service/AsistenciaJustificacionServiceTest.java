package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;

@ExtendWith(MockitoExtension.class)
class AsistenciaJustificacionServiceTest {

  @Mock AsistenciaEstudianteRepository asistencias;
  @Mock AccesoAsistenciaService acceso;
  @Mock EvidenciaStorageService storage;
  @InjectMocks AsistenciaJustificacionService service;

  private AsistenciaEstudiante asistencia;

  @BeforeEach
  void preparar() {
    Estudiante estudiante = Mockito.mock(Estudiante.class);
    Mockito.lenient().when(estudiante.getId()).thenReturn(8);
    asistencia = new AsistenciaEstudiante();
    asistencia.setEstudiante(estudiante);
    asistencia.marcarAusente();
    Mockito.lenient()
        .when(asistencias.findByIdAndActiveTrue(2))
        .thenReturn(Optional.of(asistencia));
    Mockito.lenient().when(acceso.exigirAccesoJustificacion(8, "apoderado")).thenReturn(estudiante);
  }

  @Test
  void apoderadoVinculadoJustificaInasistenciaConDocumento() {
    when(storage.guardar(any()))
        .thenReturn(
            new EvidenciaStorageService.ArchivoGuardado(
                "8fca.pdf", "constancia.pdf", "application/pdf", 512L));

    var response = service.justificar(2, "Cita médica", documento(), "apoderado");

    assertEquals("JUSTIFICADA", response.estado().name());
    assertEquals("Cita médica", response.motivo());
    assertEquals("constancia.pdf", response.documentoNombre());
    assertEquals(512L, response.documentoTamanio());
    assertEquals("8fca.pdf", asistencia.getArchivoJustificacion());
  }

  @Test
  void requiereUnMotivoNoVacio() {
    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.justificar(2, " ", documento(), "apoderado"));

    assertEquals(400, error.getStatusCode().value());
    verify(storage, never()).guardar(any());
  }

  @Test
  void requiereAdjuntarDocumentoDeSustento() {
    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.justificar(2, "Cita médica", null, "apoderado"));

    assertEquals(400, error.getStatusCode().value());
    verify(storage, never()).guardar(any());
  }

  @Test
  void soloPermiteJustificarRegistrosMarcadosAusentes() {
    asistencia.marcarPresente();

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.justificar(2, "Cita médica", documento(), "apoderado"));

    assertEquals(409, error.getStatusCode().value());
    verify(storage, never()).guardar(any());
  }

  @Test
  void rechazaApoderadosQueNoEstanVinculadosAlAlumno() {
    when(acceso.exigirAccesoJustificacion(8, "otro-apoderado"))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin vínculo"));

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.justificar(2, "Cita médica", documento(), "otro-apoderado"));

    assertEquals(403, error.getStatusCode().value());
    verify(storage, never()).guardar(any());
  }

  @Test
  void respondeNotFoundSiLaAsistenciaNoExiste() {
    when(asistencias.findByIdAndActiveTrue(99)).thenReturn(Optional.empty());

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () -> service.justificar(99, "Cita médica", documento(), "apoderado"));

    assertEquals(404, error.getStatusCode().value());
  }

  private MockMultipartFile documento() {
    return new MockMultipartFile(
        "documento", "constancia.pdf", "application/pdf", new byte[] {1, 2, 3});
  }
}
