package pe.edu.pucp.colegio.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.service.impl.CargaMasivaUsuariosServiceImpl;

@ExtendWith(MockitoExtension.class)
class CargaMasivaUsuariosServiceImplTest {

  @Mock LectorCsvUsuarios lector;
  @Mock PersonaRepository personas;
  @Mock CuentaUsuarioRepository cuentas;
  @Mock EstudianteRepository estudiantes;
  @Mock ApoderadoRepository apoderados;
  @Mock VinculoFamiliarRepository vinculos;
  @Mock RegistroAuditoriaRepository auditorias;
  @Mock PasswordEncoder encoder;

  @InjectMocks CargaMasivaUsuariosServiceImpl servicio;

  private static FilaUsuarioCsv docente(int fila, String dni, String correo) {
    return new FilaUsuarioCsv(
        fila, "DOCENTE", dni, "Ana", "Perez", "Diaz", correo, null, null, null, null);
  }

  @Test
  void cargar_conDniExistenteEnBd_retornaErrorYNoGuardaNada() {
    when(lector.leer(any())).thenReturn(List.of(docente(1, "12345678", "ana@colegio.edu.pe")));
    when(personas.existsByDni("12345678")).thenReturn(true);

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertFalse(resultado.exitoso());
    assertEquals(1, resultado.errores().size());
    assertEquals("dni", resultado.errores().get(0).campo());
    verify(personas, never()).save(any());
    verify(cuentas, never()).save(any());
  }

  @Test
  void cargar_conDniRepetidoEnElArchivo_reportaElErrorEnLaSegundaFila() {
    when(lector.leer(any()))
        .thenReturn(
            List.of(
                docente(1, "12345678", "ana@colegio.edu.pe"),
                docente(2, "12345678", "luis@colegio.edu.pe")));

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertFalse(resultado.exitoso());
    assertEquals(2, resultado.errores().get(0).fila());
    assertEquals("dni", resultado.errores().get(0).campo());
    verify(cuentas, never()).save(any());
  }

  @Test
  void cargar_alumnoSinApoderadoNiParentesco_retornaErroresDeVinculo() {
    FilaUsuarioCsv alumno =
        new FilaUsuarioCsv(
            1,
            "ALUMNO",
            "70333444",
            "Luis",
            "Torres",
            "Quispe",
            "luis@colegio.edu.pe",
            null,
            null,
            null,
            null);
    when(lector.leer(any())).thenReturn(List.of(alumno));

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertFalse(resultado.exitoso());
    List<String> campos = resultado.errores().stream().map(e -> e.campo()).toList();
    assertTrue(campos.contains("dni_apoderado"));
    assertTrue(campos.contains("parentesco"));
  }

  @Test
  void cargar_conRolNoPermitido_retornaErrorDeRol() {
    FilaUsuarioCsv admin =
        new FilaUsuarioCsv(
            1,
            "ADMINISTRADOR",
            "12345678",
            "Ana",
            "Perez",
            "Diaz",
            "ana@colegio.edu.pe",
            null,
            null,
            null,
            null);
    when(lector.leer(any())).thenReturn(List.of(admin));

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertFalse(resultado.exitoso());
    assertEquals("rol", resultado.errores().get(0).campo());
  }

  @Test
  void cargar_conDocenteValido_guardaCuentaGeneraContrasenhaYAudita() {
    when(lector.leer(any())).thenReturn(List.of(docente(1, "12345678", "Ana@Colegio.edu.pe")));
    when(personas.save(any(Persona.class))).thenAnswer(i -> i.getArgument(0));
    when(encoder.encode(any())).thenReturn("hash");

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertTrue(resultado.exitoso());
    assertEquals(1, resultado.cuentas().size());
    assertEquals("ana@colegio.edu.pe", resultado.cuentas().get(0).username());
    assertEquals(12, resultado.cuentas().get(0).contrasenhaTemporal().length());
    verify(cuentas).save(any(CuentaUsuario.class));
    verify(auditorias).save(any(RegistroAuditoria.class));
  }
}
