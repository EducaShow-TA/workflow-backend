package pe.edu.pucp.colegio.common.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RegistroErrorServiceTest {

  @Mock RegistroErrorRepository errores;
  @Mock CuentaUsuarioRepository cuentas;
  @InjectMocks RegistroErrorService servicio;

  @AfterEach
  void limpiarContextoDeSeguridad() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void registrar_errorDelServidor_guardaTrazaYModulo() {
    servicio.registrar(
        NivelLog.ERROR,
        "SIS_500",
        "GET",
        "/api/v1/estudiantes/5",
        500,
        "Ocurrió un error interno",
        new IllegalStateException("boom"));

    ArgumentCaptor<RegistroError> captor = ArgumentCaptor.forClass(RegistroError.class);
    verify(errores).save(captor.capture());
    assertEquals("estudiantes", captor.getValue().getModulo());
    assertTrue(captor.getValue().getStackTrace().contains("IllegalStateException: boom"));
  }

  @Test
  void registrar_errorDelCliente_noGuardaTraza() {
    servicio.registrar(
        NivelLog.INFO, "RES_404", "GET", "/api/v1/x", 404, "no", new RuntimeException());

    ArgumentCaptor<RegistroError> captor = ArgumentCaptor.forClass(RegistroError.class);
    verify(errores).save(captor.capture());
    assertNull(captor.getValue().getStackTrace());
  }

  @Test
  void registrar_trazaMuyLarga_seTruncaA4000() {
    Throwable larga = new RuntimeException("x".repeat(10_000));
    servicio.registrar(NivelLog.ERROR, "SIS_500", "GET", "/api/v1/a", 500, "m", larga);

    ArgumentCaptor<RegistroError> captor = ArgumentCaptor.forClass(RegistroError.class);
    verify(errores).save(captor.capture());
    assertEquals(4000, captor.getValue().getStackTrace().length());
  }

  @Test
  void registrar_fallaElRepositorio_noPropagaLaExcepcion() {
    when(errores.save(any())).thenThrow(new RuntimeException("bd caída"));

    assertDoesNotThrow(
        () -> servicio.registrar(NivelLog.ERROR, "SIS_500", "GET", "/api/v1/a", 500, "m", null));
  }
}
