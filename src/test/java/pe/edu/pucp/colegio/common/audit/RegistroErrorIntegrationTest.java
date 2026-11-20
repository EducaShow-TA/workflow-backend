package pe.edu.pucp.colegio.common.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "logs-test", roles = "ADMINISTRADOR")
class RegistroErrorIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired RegistroErrorRepository errores;
  @Autowired RegistroErrorService registroError;

  @Test
  void rutaInexistente_retorna404SinRegistrarla() throws Exception {
    long antes = errores.count();

    mvc.perform(get("/api/v1/ruta-inexistente-5555"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));

    assertEquals(antes, errores.count());
  }

  @Test
  void metodoNoPermitido_retorna405SinRegistrarlo() throws Exception {
    long antes = errores.count();

    mvc.perform(delete("/api/v1/auth/login")).andExpect(status().isMethodNotAllowed());

    assertEquals(antes, errores.count());
  }

  @Test
  void consulta_filtraPorModuloYNoExponeTraza() throws Exception {
    registroError.registrar(
        NivelLog.ERROR,
        "SIS_500",
        "GET",
        "/api/v1/ruta-inexistente-7777",
        500,
        "Falla técnica de prueba",
        new IllegalStateException("detalle que no debe exponerse"));

    mvc.perform(get("/api/v1/logs/errores").param("modulo", "ruta-inexistente-7777"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].codigoError").value("SIS_500"))
        .andExpect(jsonPath("$.content[0].stackTrace").doesNotExist());
  }

  @Test
  void consulta_nivelInvalido_retorna400() throws Exception {
    mvc.perform(get("/api/v1/logs/errores").param("nivel", "XYZ"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "DOCENTE")
  void consulta_docente_retorna403() throws Exception {
    mvc.perform(get("/api/v1/logs/errores")).andExpect(status().isForbidden());
  }

  @Test
  @WithAnonymousUser
  void sinToken_retorna401SinRegistrarlo() throws Exception {
    long antes = errores.count();

    mvc.perform(get("/api/v1/cuentas-usuario"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));

    assertEquals(antes, errores.count());
  }
}
