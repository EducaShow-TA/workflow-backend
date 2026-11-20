package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "apoderados-test", roles = "ADMINISTRADOR")
@Transactional
class ApoderadosEstudianteIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired PersonaRepository personas;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired PasswordEncoder passwordEncoder;
  @PersistenceContext EntityManager entityManager;

  @Test
  void verificarClasificaCadaDocumentoYRegistraLaConsulta() throws Exception {
    String dniMadre = dniAleatorio();
    String dniLucia = dniAleatorio();
    Integer lucia = crearEstudiante(dniLucia, dniMadre);
    Integer mateo = crearEstudiante(dniAleatorio(), dniAleatorio());

    verificar(mateo, "DNI", dniMadre)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.existe").value(true))
        .andExpect(jsonPath("$.puedeVincularse").value(true))
        .andExpect(jsonPath("$.nombreCompleto").value(containsString("Rosa")))
        .andExpect(jsonPath("$.celularEnmascarado").value("9••••4321"))
        .andExpect(jsonPath("$.correoEnmascarado").value("r•••@ejemplo.edu.pe"))
        .andExpect(jsonPath("$.tieneCuenta").value(true));

    verificar(lucia, "DNI", dniMadre)
        .andExpect(jsonPath("$.puedeVincularse").value(false))
        .andExpect(jsonPath("$.motivo").value(containsString("ya está vinculado")));

    verificar(lucia, "DNI", dniAleatorio())
        .andExpect(jsonPath("$.existe").value(false))
        .andExpect(jsonPath("$.puedeVincularse").value(true))
        .andExpect(jsonPath("$.nombreCompleto").value(nullValue()));

    verificar(lucia, "DNI", dniLucia)
        .andExpect(jsonPath("$.puedeVincularse").value(false))
        .andExpect(jsonPath("$.motivo").value(containsString("su propio apoderado")));

    verificar(mateo, "PASAPORTE", dniMadre)
        .andExpect(jsonPath("$.puedeVincularse").value(false))
        .andExpect(jsonPath("$.motivo").value(containsString("tipo de documento")))
        .andExpect(jsonPath("$.nombreCompleto").value(nullValue()))
        .andExpect(jsonPath("$.celularEnmascarado").value(nullValue()));

    verificar(mateo, "DNI", "123").andExpect(status().isBadRequest());

    Number consultas =
        (Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM registros_auditoria WHERE accion = ?1 AND descripcion"
                        + " LIKE ?2")
                .setParameter(1, AccionAuditoria.CONSULTAR_DOCUMENTO.name())
                .setParameter(2, "%" + dniMadre + "%")
                .getSingleResult();
    assertEquals(3, consultas.intValue());
  }

  @Test
  void verificarNoExponeDatosDeUnaPersonaConCuentaDeOtroRol() throws Exception {
    Integer estudiante = crearEstudiante(dniAleatorio(), dniAleatorio());
    String dniDocente = dniAleatorio();
    Persona docente = personas.save(new Persona(dniDocente, "Carla", "Ruiz", "Soto"));
    docente.setTelefono("987000111");
    cuentas.save(
        new CuentaUsuario(
            "docente" + dniDocente, passwordEncoder.encode("Clave123!x"), Rol.DOCENTE, docente));

    verificar(estudiante, "DNI", dniDocente)
        .andExpect(jsonPath("$.existe").value(true))
        .andExpect(jsonPath("$.puedeVincularse").value(false))
        .andExpect(jsonPath("$.motivo").value(containsString("otro rol")))
        .andExpect(jsonPath("$.nombreCompleto").value(nullValue()))
        .andExpect(jsonPath("$.celularEnmascarado").value(nullValue()));
  }

  @Test
  @WithMockUser(username = "docente-test", roles = "DOCENTE")
  void soloElAdministradorPuedeVerificarDocumentos() throws Exception {
    verificar(1, "DNI", dniAleatorio()).andExpect(status().isForbidden());
  }

  @Test
  void vincularReutilizaLaPersonaRegistradaYExigeDatosSoloAUnApoderadoNuevo() throws Exception {
    String dniMadre = dniAleatorio();
    crearEstudiante(dniAleatorio(), dniMadre);
    Integer hermano = crearEstudiante(dniAleatorio(), dniAleatorio());

    vincular(
            hermano,
            """
            {"tipoDocumento":"DNI","numeroDocumento":"%s","parentesco":"MADRE","esPrincipal":false}
            """
                .formatted(dniMadre))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.celular").value("987654321"))
        .andExpect(jsonPath("$.esPrincipal").value(false))
        .andExpect(jsonPath("$.contrasenhaTemporal").value(nullValue()));

    vincular(
            hermano,
            """
            {"tipoDocumento":"DNI","numeroDocumento":"%s","parentesco":"MADRE","esPrincipal":false}
            """
                .formatted(dniMadre))
        .andExpect(status().isConflict());

    String dniPadre = dniAleatorio();
    vincular(
            hermano,
            """
            {"tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Jorge","apellidoPaterno":"Quispe","apellidoMaterno":"León","parentesco":"PADRE","esPrincipal":false}
            """
                .formatted(dniPadre))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("el celular es obligatorio")));

    vincular(
            hermano,
            """
            {"tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Jorge","apellidoPaterno":"Quispe","apellidoMaterno":"León","celular":"912345678","parentesco":"PADRE","esPrincipal":true}
            """
                .formatted(dniPadre))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value(notNullValue()))
        .andExpect(jsonPath("$.contrasenhaTemporal").value(notNullValue()))
        .andExpect(jsonPath("$.esPrincipal").value(true));

    JsonNode lista = listarApoderados(hermano);
    assertEquals(3, lista.size());
    long principales = 0;
    for (JsonNode apoderado : lista) {
      if (apoderado.get("esPrincipal").asBoolean()) {
        principales++;
      }
    }
    assertEquals(1, principales);
  }

  @Test
  void elPrincipalSoloCambiaDesignandoAOtroApoderado() throws Exception {
    String dniMadre = dniAleatorio();
    Integer estudiante = crearEstudiante(dniAleatorio(), dniMadre);
    String dniPadre = dniAleatorio();
    vincular(
            estudiante,
            """
            {"tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Juan","apellidoPaterno":"Quispe","apellidoMaterno":"Mamani","celular":"987654320","parentesco":"PADRE","esPrincipal":false}
            """
                .formatted(dniPadre))
        .andExpect(status().isCreated());

    Integer madre = apoderadoId(estudiante, dniMadre);
    Integer padre = apoderadoId(estudiante, dniPadre);

    actualizar(estudiante, madre, "MADRE", false)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(containsString("apoderado principal")));

    actualizar(estudiante, padre, "PADRE", true).andExpect(status().isOk());

    JsonNode lista = listarApoderados(estudiante);
    for (JsonNode apoderado : lista) {
      boolean esPadre = apoderado.get("apoderadoId").asInt() == padre;
      assertEquals(esPadre, apoderado.get("esPrincipal").asBoolean());
    }
    assertFalse(lista.isEmpty());
  }

  private Integer crearEstudiante(String dni, String dniApoderado) throws Exception {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    String cuerpo =
        """
        {"username":"alumno%s","password":"ClaveSegura123!","tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Ana","apellidoPaterno":"Pérez","apellidoMaterno":"Díaz","apoderados":[{"tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Rosa","apellidoPaterno":"Quispe","apellidoMaterno":"Mamani","celular":"987654321","correoElectronico":"r%s@ejemplo.edu.pe","parentesco":"MADRE","esPrincipal":true}]}
        """
            .formatted(sufijo, dni, dniApoderado, dniApoderado);
    String respuesta =
        mvc.perform(
                post("/api/v1/estudiantes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(respuesta).get("estudianteId").asInt();
  }

  private ResultActions verificar(Integer estudianteId, String tipo, String numero)
      throws Exception {
    return mvc.perform(
        post("/api/v1/estudiantes/{id}/apoderados/verificacion", estudianteId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"tipoDocumento":"%s","numeroDocumento":"%s"}
                """
                    .formatted(tipo, numero)));
  }

  private ResultActions vincular(Integer estudianteId, String cuerpo) throws Exception {
    return mvc.perform(
        post("/api/v1/estudiantes/{id}/apoderados", estudianteId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
  }

  private ResultActions actualizar(
      Integer estudianteId, Integer apoderadoId, String parentesco, boolean principal)
      throws Exception {
    return mvc.perform(
        put("/api/v1/estudiantes/{id}/apoderados/{apoderadoId}", estudianteId, apoderadoId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"parentesco":"%s","esPrincipal":%s}
                """
                    .formatted(parentesco, principal)));
  }

  private JsonNode listarApoderados(Integer estudianteId) throws Exception {
    String respuesta =
        mvc.perform(get("/api/v1/estudiantes/{id}/apoderados", estudianteId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(respuesta);
  }

  private Integer apoderadoId(Integer estudianteId, String dni) throws Exception {
    for (JsonNode apoderado : listarApoderados(estudianteId)) {
      if (dni.equals(apoderado.get("numeroDocumento").asText())) {
        return apoderado.get("apoderadoId").asInt();
      }
    }
    throw new AssertionError("No se encontró al apoderado " + dni);
  }

  private static String dniAleatorio() {
    long valor = Math.abs(UUID.randomUUID().getMostSignificantBits() % 90_000_000L);
    return String.valueOf(10_000_000L + valor);
  }
}
