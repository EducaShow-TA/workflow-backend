package pe.edu.pucp.colegio.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CargaMasivaUsuariosIntegrationTest {

  private static final String URL = "/api/v1/cuentas-usuario/carga-masiva";
  private static final String CABECERA =
      "rol,dni,nombres,apellido_paterno,apellido_materno,correo,telefono,fecha_nacimiento,"
          + "dni_apoderado,parentesco\n";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CuentaUsuarioRepository cuentas;

  private static String dniAleatorio() {
    return String.valueOf(10_000_000 + ThreadLocalRandom.current().nextInt(89_000_000));
  }

  private static String sufijo() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private String login(String usuario, String contrasenha) throws Exception {
    String cuerpo =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"" + usuario + "\",\"password\":\"" + contrasenha + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(cuerpo).get("token").asText();
  }

  private static MockMultipartFile archivo(String csv) {
    return new MockMultipartFile(
        "archivo", "usuarios.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void cargaValida_creaLasTresCuentasYElDocenteCreadoNoPuedeCargar() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String s = sufijo();
    String dniApoderado = dniAleatorio();
    String dniDocente = dniAleatorio();
    String csv =
        CABECERA
            + "ALUMNO,"
            + dniAleatorio()
            + ",Luis,Torres,Quispe,al"
            + s
            + "@colegio.edu.pe,,2011-05-14,"
            + dniApoderado
            + ",MADRE\n"
            + "APODERADO,"
            + dniApoderado
            + ",Rosa,Quispe,Mamani,ap"
            + s
            + "@correo.com,987654321,,,\n"
            + "DOCENTE,"
            + dniDocente
            + ",Carlos,Vega,Rios,do"
            + s
            + "@colegio.edu.pe,,,,\n";

    String cuerpo =
        mvc.perform(multipart(URL).file(archivo(csv)).header("Authorization", "Bearer " + admin))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.exitoso").value(true))
            .andExpect(jsonPath("$.cuentas.length()").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String contrasenhaDocente = null;
    for (JsonNode cuenta : json.readTree(cuerpo).get("cuentas")) {
      if ("DOCENTE".equals(cuenta.get("rol").asText())) {
        contrasenhaDocente = cuenta.get("contrasenhaTemporal").asText();
      }
    }
    assertNotNull(contrasenhaDocente);

    String docente = login("do" + s + "@colegio.edu.pe", contrasenhaDocente);
    mvc.perform(multipart(URL).file(archivo(csv)).header("Authorization", "Bearer " + docente))
        .andExpect(status().isForbidden());
  }

  @Test
  void cargaConDniRepetido_retorna422YNoCreaNingunaCuenta() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String s = sufijo();
    String dni = dniAleatorio();
    String csv =
        CABECERA
            + "DOCENTE,"
            + dni
            + ",Ana,Perez,Diaz,uno"
            + s
            + "@colegio.edu.pe,,,,\n"
            + "DOCENTE,"
            + dni
            + ",Luis,Rojas,Soto,dos"
            + s
            + "@colegio.edu.pe,,,,\n";

    mvc.perform(multipart(URL).file(archivo(csv)).header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.exitoso").value(false))
        .andExpect(jsonPath("$.errores[0].fila").value(2))
        .andExpect(jsonPath("$.errores[0].campo").value("dni"));

    assertFalse(cuentas.existsByUsername("uno" + s + "@colegio.edu.pe"));
  }

  @Test
  void cargaConAlumnoCuyoApoderadoNoExiste_retorna422() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String csv =
        CABECERA
            + "ALUMNO,"
            + dniAleatorio()
            + ",Luis,Torres,Quispe,al"
            + sufijo()
            + "@colegio.edu.pe,,,99999999,PADRE\n";

    mvc.perform(multipart(URL).file(archivo(csv)).header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errores[0].campo").value("dni_apoderado"));
  }

  @Test
  void cargaSinArchivo_retorna422ConFormatoEstandar() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");

    mvc.perform(multipart(URL).header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422));
  }

  @Test
  void cargaSinToken_retorna401() throws Exception {
    mvc.perform(multipart(URL).file(archivo(CABECERA))).andExpect(status().isUnauthorized());
  }
}
