package pe.edu.pucp.colegio.seguridad;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.colegio.common.audit.RegistroErrorRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CuentaUsuarioIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired RegistroErrorRepository errores;

  private String login(String user, String pass) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("token").asText();
  }

  private String cuenta(String sufijo, String dni, String rol) {
    return "{\"dni\":\"" + dni + "\",\"nombre\":\"Ana\",\"apellidoPaterno\":\"Perez\","
        + "\"apellidoMaterno\":\"Diaz\",\"username\":\"u" + sufijo + "@colegio.edu.pe\","
        + "\"password\":\"Clave2026!\",\"rol\":\"" + rol + "\"}";
  }

  @Test
  void administradorCreaCuentaYLaListaYDuplicadoRetorna409ConRegistroError() throws Exception {
    String token = login("admin@educore.edu.pe", "Admin2026!");
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    String dni = sufijo;

    mvc.perform(
            post("/api/v1/cuentas-usuario")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(sufijo, dni, "DOCENTE")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("DOCENTE"));

    mvc.perform(get("/api/v1/cuentas-usuario").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.dni=='" + dni + "')]").exists());

    long antes = errores.count();
    mvc.perform(
            post("/api/v1/cuentas-usuario")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(sufijo, dni, "DOCENTE")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.path").value("/api/v1/cuentas-usuario"));
    assertTrue(errores.count() > antes, "El conflicto debe quedar en registros_error");
  }

  @Test
  void docenteNoPuedeCrearCuentas() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(
            post("/api/v1/cuentas-usuario")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(sufijo, sufijo, "DOCENTE")))
        .andExpect(status().isCreated());

    String docente = login("u" + sufijo + "@colegio.edu.pe", "Clave2026!");
    mvc.perform(get("/api/v1/cuentas-usuario").header("Authorization", "Bearer " + docente))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinTokenRetorna401() throws Exception {
    mvc.perform(get("/api/v1/cuentas-usuario")).andExpect(status().isUnauthorized());
  }
}
