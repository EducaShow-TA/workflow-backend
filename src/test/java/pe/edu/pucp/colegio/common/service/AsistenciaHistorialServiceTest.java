package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;

@ExtendWith(MockitoExtension.class)
class AsistenciaHistorialServiceTest {

  @Mock AsistenciaEstudianteRepository asistencias;
  @Mock AccesoAsistenciaService acceso;
  @InjectMocks AsistenciaHistorialService service;

  @Test
  void alumnoAutorizadoPuedeConsultarSuHistorial() {
    Estudiante estudiante = new Estudiante();
    when(acceso.exigirAccesoHistorial(3, "alumno3")).thenReturn(estudiante);
    when(asistencias.buscarHistorial(3, null, null)).thenReturn(List.of());

    List<?> result = service.consultar(3, null, null, "alumno3");

    assertEquals(0, result.size());
    verify(acceso).exigirAccesoHistorial(3, "alumno3");
  }

  @Test
  void consultaHistorialDentroDelRangoSolicitado() {
    LocalDate desde = LocalDate.of(2026, 3, 1);
    LocalDate hasta = LocalDate.of(2026, 3, 31);
    when(acceso.exigirAccesoHistorial(8, "apoderado8")).thenReturn(new Estudiante());
    when(asistencias.buscarHistorial(8, desde, hasta)).thenReturn(List.of());

    service.consultar(8, desde, hasta, "apoderado8");

    verify(asistencias).buscarHistorial(8, desde, hasta);
  }

  @Test
  void convierteElRegistroEnUnaEntradaDelHistorial() {
    AsistenciaEstudiante asistencia = asistenciaConSesion();
    when(acceso.exigirAccesoHistorial(4, "alumno4")).thenReturn(new Estudiante());
    when(asistencias.buscarHistorial(4, null, null)).thenReturn(List.of(asistencia));

    var respuesta = service.consultar(4, null, null, "alumno4").getFirst();

    assertEquals(LocalDate.of(2026, 3, 5), respuesta.fecha());
    assertEquals(LocalTime.of(9, 0), respuesta.horaInicio());
    assertEquals("MAT", respuesta.areaCodigo());
    assertEquals("Matemática", respuesta.areaNombre());
    assertEquals("Carlos Docente Prueba", respuesta.docenteNombre());
  }

  @Test
  void incluyeRutaYNombreDelDocumentoJustificante() {
    AsistenciaEstudiante asistencia = asistenciaConSesion();
    asistencia.justificar("Cita médica", "clave-documento.pdf");
    asistencia.setDocumentoJustificacion(
        "clave-documento.pdf", "constancia.pdf", "application/pdf", 120L);
    when(acceso.exigirAccesoHistorial(4, "apoderado4")).thenReturn(new Estudiante());
    when(asistencias.buscarHistorial(4, null, null)).thenReturn(List.of(asistencia));

    var respuesta = service.consultar(4, null, null, "apoderado4").getFirst();

    assertEquals("constancia.pdf", respuesta.documentoNombre());
    assertEquals(
        "/api/v1/asistencias-estudiantes/19/justificacion/documento", respuesta.documentoUrl());
  }

  @Test
  void rechazaUnRangoDeFechasInvertidoAntesDeConsultar() {
    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.consultar(
                    4, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1), "alumno4"));

    assertEquals(400, error.getStatusCode().value());
    verify(acceso, never()).exigirAccesoHistorial(4, "alumno4");
    verify(asistencias, never()).buscarHistorial(4, null, null);
  }

  private AsistenciaEstudiante asistenciaConSesion() {
    Area area = new Area();
    area.setCodigo("MAT");
    area.setNombre("Matemática");

    Seccion seccion = new Seccion();
    seccion.setCodigo("B");

    Docente docente = new Docente();
    docente.setPersona(new Persona("10000001", "Carlos", "Docente", "Prueba"));

    SesionClase sesion = new SesionClase();
    sesion.setFecha(LocalDate.of(2026, 3, 5));
    sesion.setHoraInicio(LocalTime.of(9, 0));
    sesion.setHoraFin(LocalTime.of(9, 45));
    sesion.setSeccion(seccion);
    sesion.setArea(area);
    sesion.setDocente(docente);

    AsistenciaEstudiante asistencia = new AsistenciaEstudiante();
    ReflectionTestUtils.setField(asistencia, "id", 19);
    asistencia.setSesionClase(sesion);
    asistencia.marcarPresente();
    return asistencia;
  }
}
