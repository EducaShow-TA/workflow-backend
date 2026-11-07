package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AccesoAsistenciaServiceTest {

  @Mock CuentaUsuarioRepository cuentas;
  @Mock EstudianteRepository estudiantes;
  @Mock ApoderadoRepository apoderados;
  @Mock VinculoFamiliarRepository vinculos;
  @InjectMocks AccesoAsistenciaService service;

  private Estudiante estudiante;

  @BeforeEach
  void prepararEstudiante() {
    Persona personaEstudiante = mock(Persona.class);
    Mockito.lenient().when(personaEstudiante.getId()).thenReturn(12);
    estudiante = mock(Estudiante.class);
    Mockito.lenient().when(estudiante.getActive()).thenReturn(true);
    Mockito.lenient().when(estudiante.getId()).thenReturn(4);
    Mockito.lenient().when(estudiante.getPersona()).thenReturn(personaEstudiante);
    Mockito.lenient().when(estudiantes.findById(4)).thenReturn(Optional.of(estudiante));
  }

  @Test
  void alumnoSoloPuedeConsultarSuPropioHistorial() {
    CuentaUsuario propio = cuenta(Rol.ALUMNO, persona(12));
    CuentaUsuario otro = cuenta(Rol.ALUMNO, persona(99));
    when(cuentas.findByUsername("alumno-propio")).thenReturn(Optional.of(propio));
    when(cuentas.findByUsername("otro-alumno")).thenReturn(Optional.of(otro));

    assertSame(estudiante, service.exigirAccesoHistorial(4, "alumno-propio"));

    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () -> service.exigirAccesoHistorial(4, "otro-alumno"))
            .getStatusCode()
            .value());
  }

  @Test
  void apoderadoSoloPuedeConsultarAlumnosConVinculoActivo() {
    CuentaUsuario cuenta = cuenta(Rol.APODERADO, persona(22));
    Apoderado apoderado = mock(Apoderado.class);
    when(apoderado.getId()).thenReturn(7);
    when(cuentas.findByUsername("apoderado")).thenReturn(Optional.of(cuenta));
    when(apoderados.findByPersonaIdAndActiveTrue(22)).thenReturn(Optional.of(apoderado));
    when(vinculos.existsByEstudianteIdAndApoderadoIdAndActiveTrue(4, 7)).thenReturn(true, false);

    assertSame(estudiante, service.exigirAccesoHistorial(4, "apoderado"));

    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class, () -> service.exigirAccesoHistorial(4, "apoderado"))
            .getStatusCode()
            .value());
  }

  @Test
  void docenteYAdministradorPuedenConsultarCualquierHistorial() {
    CuentaUsuario docente = cuenta(Rol.DOCENTE, persona(88));
    CuentaUsuario admin = cuenta(Rol.ADMINISTRADOR, persona(89));
    when(cuentas.findByUsername("docente")).thenReturn(Optional.of(docente));
    when(cuentas.findByUsername("admin")).thenReturn(Optional.of(admin));

    assertSame(estudiante, service.exigirAccesoHistorial(4, "docente"));
    assertSame(estudiante, service.exigirAccesoHistorial(4, "admin"));
  }

  @Test
  void noPermiteJustificarPorOtroAlumnoNiPorPersonalDelColegio() {
    CuentaUsuario otroAlumno = cuenta(Rol.ALUMNO, persona(99));
    CuentaUsuario docente = cuenta(Rol.DOCENTE, persona(88));
    when(cuentas.findByUsername("otro-alumno")).thenReturn(Optional.of(otroAlumno));
    when(cuentas.findByUsername("docente")).thenReturn(Optional.of(docente));

    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () -> service.exigirAccesoJustificacion(4, "otro-alumno"))
            .getStatusCode()
            .value());
    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () -> service.exigirAccesoJustificacion(4, "docente"))
            .getStatusCode()
            .value());
  }

  @Test
  void rechazaCuentasInactivas() {
    CuentaUsuario cuenta = cuenta(Rol.ALUMNO, persona(12));
    when(cuenta.getActive()).thenReturn(false);
    when(cuentas.findByUsername("inactivo")).thenReturn(Optional.of(cuenta));

    assertEquals(
        401,
        assertThrows(
                ResponseStatusException.class, () -> service.exigirAccesoHistorial(4, "inactivo"))
            .getStatusCode()
            .value());
  }

  private CuentaUsuario cuenta(Rol rol, Persona persona) {
    CuentaUsuario cuenta = mock(CuentaUsuario.class);
    Mockito.lenient().when(cuenta.getActive()).thenReturn(true);
    Mockito.lenient().when(cuenta.getRol()).thenReturn(rol);
    Mockito.lenient().when(cuenta.getPersona()).thenReturn(persona);
    return cuenta;
  }

  private Persona persona(Integer id) {
    Persona persona = mock(Persona.class);
    Mockito.lenient().when(persona.getId()).thenReturn(id);
    return persona;
  }
}
