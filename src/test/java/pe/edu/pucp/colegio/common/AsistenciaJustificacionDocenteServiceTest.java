package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.service.AccesoAsistenciaService;
import pe.edu.pucp.colegio.common.service.AsistenciaJustificacionService;
import pe.edu.pucp.colegio.common.service.EvidenciaStorageService;

@ExtendWith(MockitoExtension.class)
class AsistenciaJustificacionDocenteServiceTest {
  @Mock AsistenciaEstudianteRepository asistencias;
  @Mock AccesoAsistenciaService acceso;
  @Mock EvidenciaStorageService storage;
  @InjectMocks AsistenciaJustificacionService servicio;

  private AsistenciaEstudiante asistencia;
  private SesionClase sesion;

  @BeforeEach
  void preparar() {
    sesion = new SesionClase();
    asistencia = new AsistenciaEstudiante();
    asistencia.setEstudiante(new Estudiante());
    asistencia.setSesionClase(sesion);
    asistencia.marcarAusente();
    when(asistencias.findByIdAndActiveTrue(7)).thenReturn(Optional.of(asistencia));
  }

  @Test
  void docenteJustificaSinArchivoYRegistraSuIdentidadYFecha() {
    var resultado = servicio.justificarPorDocente(7, "  Cita médica  ", null, "docente01");
    assertEquals(EstadoAsistencia.JUSTIFICADA, resultado.estado());
    assertEquals("Cita médica", asistencia.getMotivoJustificacion());
    assertNull(asistencia.getArchivoJustificacion());
    assertNull(resultado.documentoUrl());
    assertEquals("docente01", asistencia.getCorregidaPor());
    assertNotNull(asistencia.getCorregidaEn());
    verify(acceso).exigirDocenteDeSesion("docente01", sesion);
    verify(storage, never()).guardar(any());
  }

  @Test
  void noPermiteJustificarUnaAsistenciaPresente() {
    asistencia.marcarPresente();
    var error =
        assertThrows(
            ResponseStatusException.class,
            () -> servicio.justificarPorDocente(7, "Motivo", null, "docente01"));
    assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    verify(storage, never()).guardar(any());
  }

  @Test
  void noPermiteJustificarUnaSesionAjena() {
    doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
        .when(acceso)
        .exigirDocenteDeSesion("docenteAjeno", sesion);
    assertThrows(
        ResponseStatusException.class,
        () -> servicio.justificarPorDocente(7, "Motivo", null, "docenteAjeno"));
    assertEquals(EstadoAsistencia.AUSENTE, asistencia.getEstadoAsistencia());
    verify(storage, never()).guardar(any());
  }
}
