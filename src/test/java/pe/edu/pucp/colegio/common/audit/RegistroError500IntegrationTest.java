package pe.edu.pucp.colegio.common.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.colegio.security.service.CuentaUsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "logs-500-test", roles = "ADMINISTRADOR")
class RegistroError500IntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired RegistroErrorRepository errores;
  @MockitoBean CuentaUsuarioService cuentas;

  @Test
  void errorInterno_retorna500SinTrazaYGuardaTrazaEnBd() throws Exception {
    when(cuentas.listar()).thenThrow(new IllegalStateException("boom-secreto"));

    String cuerpo =
        mvc.perform(get("/api/v1/cuentas-usuario"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Ocurrió un error interno"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(cuerpo.contains("boom-secreto"));

    RegistroError ultimo = errores.findAll(Sort.by(Sort.Direction.DESC, "id")).get(0);
    assertEquals("SIS_500", ultimo.getCodigoError());
    assertEquals("cuentas-usuario", ultimo.getModulo());
    assertTrue(ultimo.getStackTrace().contains("boom-secreto"));

    mvc.perform(get("/api/v1/logs/errores/{id}", ultimo.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stackTrace").isNotEmpty());
  }
}
