package pe.edu.pucp.colegio.security;

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
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CuentaUsuarioIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired RegistroErrorRepository errores;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired EstudianteRepository estudiantes;
  @Autowired ApoderadoRepository apoderados;

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
    return "{\"dni\":\""
        + dni
        + "\",\"nombre\":\"Ana\",\"apellidoPaterno\":\"Perez\","
        + "\"apellidoMaterno\":\"Diaz\",\"username\":\"u"
        + sufijo
        + "@colegio.edu.pe\","
        + "\"password\":\"Clave2026!\",\"rol\":\""
        + rol
        + "\"}";
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
    mvc.perform(get("/api/v1/periodos/1/vacantes").header("Authorization", "Bearer " + docente))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/areas").header("Authorization", "Bearer " + docente))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/secciones").header("Authorization", "Bearer " + docente))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/horarios/plantillas").header("Authorization", "Bearer " + docente))
        .andExpect(status().isForbidden());
  }

  @Test
  void administradorActualizaDatosPersonalesYCorreoSinCambiarRolNiDni() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    String dni = sufijo;
    String body =
        mvc.perform(
                post("/api/v1/cuentas-usuario")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuenta(sufijo, dni, "DOCENTE")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = json.readTree(body).get("id").asInt();

    mvc.perform(
            put("/api/v1/cuentas-usuario/{id}", id)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Andrea\",\"apellidoPaterno\":\"Lopez\","
                        + "\"apellidoMaterno\":\"Rojas\",\"username\":\"actualizada"
                        + sufijo
                        + "@colegio.edu.pe\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("actualizada" + sufijo + "@colegio.edu.pe"))
        .andExpect(jsonPath("$.rol").value("DOCENTE"))
        .andExpect(jsonPath("$.dni").value(dni))
        .andExpect(jsonPath("$.nombre").value("Andrea"))
        .andExpect(jsonPath("$.apellidoPaterno").value("Lopez"))
        .andExpect(jsonPath("$.apellidoMaterno").value("Rojas"));
  }

  @Test
  void docenteNoPuedeActualizarCuenta() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    String body =
        mvc.perform(
                post("/api/v1/cuentas-usuario")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuenta(sufijo, sufijo, "DOCENTE")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = json.readTree(body).get("id").asInt();
    String docente = login("u" + sufijo + "@colegio.edu.pe", "Clave2026!");

    mvc.perform(
            put("/api/v1/cuentas-usuario/{id}", id)
                .header("Authorization", "Bearer " + docente)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Andrea\",\"apellidoPaterno\":\"Lopez\","
                        + "\"apellidoMaterno\":\"Rojas\",\"username\":\"otro"
                        + sufijo
                        + "@colegio.edu.pe\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void sinTokenRetorna401() throws Exception {
    mvc.perform(get("/api/v1/cuentas-usuario")).andExpect(status().isUnauthorized());
  }

  @Test
  void desactivarCuentaImpideLoginYUsoDeTokenPrevio() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    String body =
        mvc.perform(
                post("/api/v1/cuentas-usuario")
                    .header("Authorization", "Bearer " + admin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuenta(sufijo, sufijo, "DOCENTE")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = json.readTree(body).get("id").asInt();
    String username = "u" + sufijo + "@colegio.edu.pe";
    String docente = login(username, "Clave2026!");

    mvc.perform(
            patch("/api/v1/cuentas-usuario/{id}/estado", id)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.activo").value(false));

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Clave2026!\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + docente))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void administradorNoPuedeDesactivarSuPropiaCuenta() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    int id = cuentas.findByUsername("admin@educore.edu.pe").orElseThrow().getId();

    mvc.perform(
            patch("/api/v1/cuentas-usuario/{id}/estado", id)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"))
        .andExpect(status().isConflict());
  }

  @Test
  void crearCuentasAlumnoYApoderadoCreaSusPerfiles() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String sufijoAlumno = UUID.randomUUID().toString().substring(0, 8);
    String sufijoApoderado = UUID.randomUUID().toString().substring(0, 8);

    mvc.perform(
            post("/api/v1/cuentas-usuario")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(sufijoAlumno, sufijoAlumno, "ALUMNO")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("ALUMNO"));
    mvc.perform(
            post("/api/v1/cuentas-usuario")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuenta(sufijoApoderado, sufijoApoderado, "APODERADO")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("APODERADO"));

    assertTrue(estudiantes.existsByPersonaDni(sufijoAlumno));
    assertTrue(apoderados.existsByPersonaDni(sufijoApoderado));
  }
}
