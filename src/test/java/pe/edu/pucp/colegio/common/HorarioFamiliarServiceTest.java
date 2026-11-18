package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.BloqueHorario;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.HorarioSeccionRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.common.service.HorarioFamiliarService;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class HorarioFamiliarServiceTest {

  @Mock CuentaUsuarioRepository cuentas;
  @Mock EstudianteRepository estudiantes;
  @Mock ApoderadoRepository apoderados;
  @Mock VinculoFamiliarRepository vinculos;
  @Mock MatriculaRepository matriculas;
  @Mock HorarioSeccionRepository horarios;
  @Mock SesionHorarioRepository sesiones;

  HorarioFamiliarService servicio;

  @BeforeEach
  void configurar() {
    var clock = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneId.of("America/Lima"));
    servicio =
        new HorarioFamiliarService(
            cuentas, estudiantes, apoderados, vinculos, matriculas, horarios, sesiones, clock);
  }

  @Test
  void administradorNoPuedeConsultarHorariosFamiliares() {
    when(cuentas.findByUsername("admin"))
        .thenReturn(Optional.of(cuenta("admin", Rol.ADMINISTRADOR)));

    var error = assertThrows(ResponseStatusException.class, () -> servicio.listar("admin", 2027));
    assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    verifyNoInteractions(horarios, sesiones);
  }

  @Test
  void alumnoSinMatriculaConfirmadaNoRecibeHorarios() {
    var cuenta = cuenta("estudiante", Rol.ALUMNO);
    Estudiante estudiante = mock(Estudiante.class);
    when(estudiante.getActive()).thenReturn(true);
    when(estudiante.getId()).thenReturn(11);
    when(cuentas.findByUsername("estudiante")).thenReturn(Optional.of(cuenta));
    when(estudiantes.findByPersonaDni(cuenta.getPersona().getDni()))
        .thenReturn(Optional.of(estudiante));
    when(matriculas.findAllByEstudianteIdIn(List.of(11))).thenReturn(List.of());

    assertTrue(servicio.listar("estudiante", 2027).isEmpty());
    verifyNoInteractions(horarios, sesiones);
  }

  @Test
  void apoderadoSoloRecibeHorarioDelHijoVinculadoYLasClasesVigentes() {
    var cuenta = cuenta("familia", Rol.APODERADO);
    Apoderado apoderado = mock(Apoderado.class);
    Estudiante hijo = mock(Estudiante.class);
    Persona personaAlumno = new Persona("11111111", "María", "Prueba", "Alumno");
    VinculoFamiliar vinculo = mock(VinculoFamiliar.class);
    Matricula matricula = mock(Matricula.class);
    Periodo periodo = mock(Periodo.class);
    Grado grado = mock(Grado.class);
    Seccion seccion = mock(Seccion.class);
    HorarioSeccion horario = mock(HorarioSeccion.class);
    SesionHorario sesion = mock(SesionHorario.class);
    BloqueHorario bloque = mock(BloqueHorario.class);
    Area area = mock(Area.class);
    Aula aula = mock(Aula.class);
    Docente docente = mock(Docente.class);

    when(cuentas.findByUsername("familia")).thenReturn(Optional.of(cuenta));
    when(apoderados.findByPersonaIdAndActiveTrue(100)).thenReturn(Optional.of(apoderado));
    when(apoderado.getId()).thenReturn(50);
    when(vinculos.listarEstudiantesActivosDeApoderado(50)).thenReturn(List.of(vinculo));
    when(vinculo.getEstudiante()).thenReturn(hijo);
    when(hijo.getId()).thenReturn(12);
    when(hijo.getPersona()).thenReturn(personaAlumno);
    when(matriculas.findAllByEstudianteIdIn(List.of(12))).thenReturn(List.of(matricula));
    when(matricula.getEstudiante()).thenReturn(hijo);
    when(matricula.getActive()).thenReturn(true);
    when(matricula.getEstado()).thenReturn(EstadoMatricula.CONFIRMADA);
    when(matricula.getPeriodo()).thenReturn(periodo);
    when(periodo.getActive()).thenReturn(true);
    when(periodo.getFechaInicio()).thenReturn(LocalDate.of(2027, 3, 1));
    when(periodo.getGrado()).thenReturn(grado);
    when(grado.getNumero()).thenReturn(3);
    when(periodo.getSeccion()).thenReturn(seccion);
    when(seccion.getId()).thenReturn(9);
    when(seccion.getCodigo()).thenReturn("A");
    when(horarios.findBySeccionIdAndAnio(9, 2027)).thenReturn(Optional.of(horario));
    when(horario.getActive()).thenReturn(true);
    when(horario.getId()).thenReturn(77);
    when(sesiones.findAllByHorarioId(77)).thenReturn(List.of(sesion));
    when(sesion.estaVigenteEn(LocalDate.of(2027, 3, 1))).thenReturn(true);
    when(sesion.getId()).thenReturn(6);
    when(sesion.getDia()).thenReturn(DayOfWeek.MONDAY);
    when(sesion.getBloque()).thenReturn(bloque);
    when(bloque.getHoraInicio()).thenReturn(LocalTime.of(8, 0));
    when(bloque.getHoraFin()).thenReturn(LocalTime.of(8, 45));
    when(sesion.getArea()).thenReturn(area);
    when(area.getNombre()).thenReturn("Matemática");
    when(sesion.getAula()).thenReturn(aula);
    when(aula.getCodigo()).thenReturn("301");
    when(sesion.getDocente()).thenReturn(docente);
    when(docente.getPersona()).thenReturn(new Persona("22222222", "Luis", "Ejemplo", "Docente"));

    var lista = servicio.listar("familia", 2027);
    assertEquals(1, lista.size());
    assertEquals(12, lista.get(0).estudianteId());
    assertTrue(lista.get(0).horarioDisponible());
    assertEquals("Matemática", lista.get(0).bloques().get(0).areaNombre());
    assertEquals("08:00", lista.get(0).bloques().get(0).horaInicio());
    assertFalse(lista.get(0).bloques().isEmpty());
    verify(horarios).findBySeccionIdAndAnio(9, 2027);
  }

  private CuentaUsuario cuenta(String username, Rol rol) {
    var persona = new Persona(username, "Persona", "De", "Prueba");
    ReflectionTestUtils.setField(persona, "id", 100);
    var cuenta = new CuentaUsuario(username, "hash", rol, persona);
    return cuenta;
  }
}
