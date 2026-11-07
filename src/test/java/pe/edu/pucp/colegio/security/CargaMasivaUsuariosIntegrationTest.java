package pe.edu.pucp.colegio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
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
  @Autowired PersonaRepository personas;
  @Autowired EstudianteRepository estudiantes;
  @Autowired ApoderadoRepository apoderados;
  @Autowired VinculoFamiliarRepository vinculos;

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
    String dniAlumno = dniAleatorio();
    String csv =
        CABECERA
            + "ALUMNO,"
            + dniAlumno
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
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.exitoso").value(true))
            .andExpect(jsonPath("$.cuentas.length()").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String contrasenhaDocente = null;
    String usernameDocente = null;
    for (JsonNode cuenta : json.readTree(cuerpo).get("cuentas")) {
      if ("DOCENTE".equals(cuenta.get("rol").asText())) {
        contrasenhaDocente = cuenta.get("contrasenhaTemporal").asText();
        usernameDocente = cuenta.get("username").asText();
      }
    }
    assertNotNull(contrasenhaDocente);
    assertEquals("usuario" + dniDocente, usernameDocente);

    String docente = login(usernameDocente, contrasenhaDocente);
    mvc.perform(multipart(URL).file(archivo(csv)).header("Authorization", "Bearer " + docente))
        .andExpect(status().isForbidden());
  }

  @Test
  void cargaSeparaUsernameDeCorreoYPermiteLoginConAmbos() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String sufijo = sufijo();
    String username = "docente" + sufijo;
    String correo = "contacto-" + sufijo + "@colegio.edu.pe";
    String dni = dniAleatorio();
    String csv =
        "rol,dni,nombres,apellido_paterno,apellido_materno,correo,telefono,fecha_nacimiento,"
            + "dni_apoderado,parentesco,username\n"
            + "DOCENTE,"
            + dni
            + ",Ana,Perez,Diaz,"
            + correo
            + ",,,,,"
            + username
            + "\n";

    String cuerpo =
        mvc.perform(multipart(URL).file(archivo(csv)).header("Authorization", "Bearer " + admin))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cuentas[0].username").value(username))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String contrasenhaTemporal =
        json.readTree(cuerpo).get("cuentas").get(0).get("contrasenhaTemporal").asText();

    assertEquals(username, cuentas.findByPersonaDni(dni).orElseThrow().getUsername());
    assertEquals(correo, personas.findByDni(dni).orElseThrow().getCorreo());
    login(username, contrasenhaTemporal);
    login(correo, contrasenhaTemporal);
  }

  @Test
  void cargaRechazaUsernameConFormatoDeCorreo() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String dni = dniAleatorio();
    String csv =
        "rol,dni,nombres,apellido_paterno,apellido_materno,correo,telefono,fecha_nacimiento,"
            + "dni_apoderado,parentesco,username\n"
            + "DOCENTE,"
            + dni
            + ",Ana,Perez,Diaz,contacto"
            + sufijo()
            + "@colegio.edu.pe,,,,,usuario@ejemplo.com\n";

    mvc.perform(multipart(URL).file(archivo(csv)).header("Authorization", "Bearer " + admin))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errores[0].campo").value("username"));
    assertFalse(cuentas.findByPersonaDni(dni).isPresent());
  }

  @Test
  void cargaConDniRepetido_procesaLaFilaValidaYRechazaLaDuplicada() throws Exception {
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
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exitoso").value(false))
        .andExpect(jsonPath("$.usuariosCreados").value(1))
        .andExpect(jsonPath("$.filasRechazadas").value(1))
        .andExpect(jsonPath("$.errores[0].fila").value(2))
        .andExpect(jsonPath("$.errores[0].campo").value("dni"));

    assertTrue(cuentas.existsByUsername("usuario" + dni));
  }

  @Test
  void cargaConPersonaExistente_actualizaDatosConservandoCuentaYContrasenha() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String s = sufijo();
    String dni = dniAleatorio();
    String correoOriginal = "docente" + s + "@colegio.edu.pe";
    String csvOriginal =
        CABECERA + "DOCENTE," + dni + ",Ana,Perez,Diaz," + correoOriginal + ",,1988-04-03,,\n";

    String cuerpoOriginal =
        mvc.perform(
                multipart(URL)
                    .file(archivo(csvOriginal))
                    .header("Authorization", "Bearer " + admin))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String contrasenhaTemporal =
        json.readTree(cuerpoOriginal).get("cuentas").get(0).get("contrasenhaTemporal").asText();
    CuentaUsuario cuentaOriginal = cuentas.findByPersonaDni(dni).orElseThrow();
    assertEquals("usuario" + dni, cuentaOriginal.getUsername());
    Integer cuentaId = cuentaOriginal.getId();
    String hashOriginal = cuentaOriginal.getPassword();

    String correoActualizado = "docente-actualizado" + s + "@colegio.edu.pe";
    String csvActualizado =
        CABECERA
            + "DOCENTE,"
            + dni
            + ",Andrea,Lopez,Rojas,"
            + correoActualizado
            + ",999111222,1988-04-03,,\n";
    mvc.perform(
            multipart(URL).file(archivo(csvActualizado)).header("Authorization", "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuariosCreados").value(0))
        .andExpect(jsonPath("$.usuariosActualizados").value(1))
        .andExpect(jsonPath("$.cuentas").isEmpty())
        .andExpect(jsonPath("$.resultados[0].estado").value("ACTUALIZADO"));

    CuentaUsuario cuentaActualizada = cuentas.findByPersonaDni(dni).orElseThrow();
    assertEquals(cuentaId, cuentaActualizada.getId());
    assertEquals(hashOriginal, cuentaActualizada.getPassword());
    assertEquals("Andrea", personas.findByDni(dni).orElseThrow().getNombre());
    assertEquals(correoActualizado, personas.findByDni(dni).orElseThrow().getCorreo());
    assertEquals("usuario" + dni, cuentaActualizada.getUsername());
    login(correoActualizado, contrasenhaTemporal);
  }

  @Test
  void cargaActualizaVinculoExistenteSinRecrearAlEstudiante() throws Exception {
    String admin = login("admin@educore.edu.pe", "Admin2026!");
    String s = sufijo();
    String dniAlumno = dniAleatorio();
    String dniApoderadoInicial = dniAleatorio();
    String correoAlumno = "alumno" + s + "@colegio.edu.pe";
    String csvInicial =
        CABECERA
            + "ALUMNO,"
            + dniAlumno
            + ",Luis,Torres,Quispe,"
            + correoAlumno
            + ",,2011-05-14,"
            + dniApoderadoInicial
            + ",MADRE\n"
            + "APODERADO,"
            + dniApoderadoInicial
            + ",Rosa,Quispe,Mamani,ap-inicial"
            + s
            + "@colegio.edu.pe,,,,\n";
    mvc.perform(multipart(URL).file(archivo(csvInicial)).header("Authorization", "Bearer " + admin))
        .andExpect(status().isCreated());

    Integer estudianteId = estudiantes.findByPersonaDni(dniAlumno).orElseThrow().getId();
    String dniApoderadoNuevo = dniAleatorio();
    String csvActualizado =
        CABECERA
            + "ALUMNO,"
            + dniAlumno
            + ",Luis,Torres,Quispe,"
            + correoAlumno
            + ",,2011-05-14,"
            + dniApoderadoNuevo
            + ",PADRE\n"
            + "APODERADO,"
            + dniApoderadoNuevo
            + ",Rosa,Quispe,Mamani,ap-nuevo"
            + s
            + "@colegio.edu.pe,,,,\n";
    mvc.perform(
            multipart(URL).file(archivo(csvActualizado)).header("Authorization", "Bearer " + admin))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.usuariosCreados").value(1))
        .andExpect(jsonPath("$.usuariosVinculados").value(1))
        .andExpect(jsonPath("$.resultados[0].estado").value("CREADO"));

    assertEquals(estudianteId, estudiantes.findByPersonaDni(dniAlumno).orElseThrow().getId());
    var apoderadoNuevo = apoderados.findByPersonaDni(dniApoderadoNuevo).orElseThrow();
    var vinculo =
        vinculos
            .findByEstudianteIdAndApoderadoId(estudianteId, apoderadoNuevo.getId())
            .orElseThrow();
    assertEquals(
        apoderados.findByPersonaDni(dniApoderadoNuevo).orElseThrow().getId(),
        vinculo.getApoderado().getId());
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
