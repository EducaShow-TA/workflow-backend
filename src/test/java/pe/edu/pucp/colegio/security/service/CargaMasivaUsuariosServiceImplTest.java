package pe.edu.pucp.colegio.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.service.impl.CargaMasivaUsuariosServiceImpl;
import pe.edu.pucp.colegio.security.service.impl.ProcesadorFilaCargaMasiva;
import pe.edu.pucp.colegio.security.service.impl.ResultadoProcesamientoFila;

@ExtendWith(MockitoExtension.class)
class CargaMasivaUsuariosServiceImplTest {

  @Mock LectorCsvUsuarios lector;
  @Mock CuentaUsuarioRepository cuentas;
  @Mock EstudianteRepository estudiantes;
  @Mock ApoderadoRepository apoderados;
  @Mock VinculoFamiliarRepository vinculos;
  @Mock RegistroAuditoriaRepository auditorias;
  @Mock PasswordEncoder encoder;
  @Mock ProcesadorFilaCargaMasiva procesador;

  @InjectMocks CargaMasivaUsuariosServiceImpl servicio;

  private static FilaUsuarioCsv docente(int fila, String dni, String correo) {
    return new FilaUsuarioCsv(
        fila, "DOCENTE", dni, "Ana", "Perez", "Diaz", correo, null, null, null, null);
  }

  @Test
  void cargar_conCuentaExistente_laActualizaSinCrearOtraCuenta() {
    FilaUsuarioCsv fila = docente(1, "12345678", "ana@colegio.edu.pe");
    CuentaUsuario existente =
        new CuentaUsuario(
            "ana-antigua@colegio.edu.pe",
            "hash-original",
            Rol.DOCENTE,
            new Persona("12345678", "Ana", "Perez", "Diaz"));
    when(lector.leer(any())).thenReturn(List.of(fila));
    when(cuentas.findByPersonaDni("12345678")).thenReturn(Optional.of(existente));
    when(procesador.procesar(fila))
        .thenReturn(
            new ResultadoProcesamientoFila(
                Rol.DOCENTE, "12345678", "ana@colegio.edu.pe", false, true, false, null));

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertTrue(resultado.exitoso());
    assertEquals(1, resultado.usuariosActualizados());
    assertEquals(0, resultado.usuariosCreados());
    verify(cuentas, never()).save(any());
  }

  @Test
  void cargar_conDniRepetidoEnElArchivo_reportaElErrorEnLaSegundaFila() {
    when(lector.leer(any()))
        .thenReturn(
            List.of(
                docente(1, "12345678", "ana@colegio.edu.pe"),
                docente(2, "12345678", "luis@colegio.edu.pe")));
    when(procesador.procesar(any(FilaUsuarioCsv.class)))
        .thenReturn(
            new ResultadoProcesamientoFila(
                Rol.DOCENTE, "12345678", "ana@colegio.edu.pe", true, false, false, "TmpClave123"));

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertFalse(resultado.exitoso());
    assertEquals(2, resultado.errores().get(0).fila());
    assertEquals("dni", resultado.errores().get(0).campo());
    assertEquals(1, resultado.usuariosCreados());
    assertEquals(1, resultado.filasRechazadas());
    verify(procesador).procesar(any(FilaUsuarioCsv.class));
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
    when(procesador.procesar(any(FilaUsuarioCsv.class)))
        .thenReturn(
            new ResultadoProcesamientoFila(
                Rol.DOCENTE, "12345678", "ana@colegio.edu.pe", true, false, false, "TmpClave123"));

    CargaMasivaResultadoDTO resultado = servicio.cargar(new byte[0], "admin@educore.edu.pe");

    assertTrue(resultado.exitoso());
    assertEquals(1, resultado.cuentas().size());
    assertEquals("ana@colegio.edu.pe", resultado.cuentas().get(0).username());
    assertEquals("TmpClave123", resultado.cuentas().get(0).contrasenhaTemporal());
    verify(procesador).procesar(any(FilaUsuarioCsv.class));
    verify(auditorias).save(any(RegistroAuditoria.class));
  }
}
