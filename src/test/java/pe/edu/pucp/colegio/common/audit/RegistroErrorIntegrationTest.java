/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package pe.edu.pucp.colegio.common.audit;

import static org.h2.store.fs.FileUtils.delete;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * @author oliver
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "logs-test", roles = "ADMINISTRADOR")
class RegistroErrorIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired RegistroErrorRepository errores;

  @Test
  void rutaInexistente_retorna404YQuedaRegistrada() throws Exception {
    long antes = errores.count();
    mvc.perform(get("/api/v1/ruta-inexistente-5555"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    assertTrue(errores.count() > antes);
  }

  @Test
  void metodoNoPermitido_retorna405() throws Exception {
    mvc.perform(delete("/api/v1/auth/login")).andExpect(status().isMethodNotAllowed());
  }

  @Test
  void consulta_filtraPorModuloYNoExponeTraza() throws Exception {
    mvc.perform(get("/api/v1/ruta-inexistente-7777")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/logs/errores").param("modulo", "ruta-inexistente-7777"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].codigoError").value("RES_404"))
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
  void sinToken_retorna401YQuedaRegistrado() throws Exception {
    long antes = errores.count();
    mvc.perform(get("/api/v1/cuentas-usuario")).andExpect(status().isUnauthorized());
    assertTrue(errores.count() > antes);
  }
}
