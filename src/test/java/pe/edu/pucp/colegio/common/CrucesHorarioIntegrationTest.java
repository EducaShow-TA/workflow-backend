package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** US-4.2: el sistema detecta e impide cruces de docente y de aula. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "cruces-horario-test", roles = "ADMINISTRADOR")
@Transactional
class CrucesHorarioIntegrationTest {

  private static final int ANIO = 2027;
  private static final String BASE = "/api/v1/horarios/secciones";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired GradoRepository grados;
  @Autowired SeccionRepository secciones;
  @Autowired AreaRepository areas;
  @Autowired AulaRepository aulas;
  @Autowired PersonaRepository personas;
  @Autowired CuentaUsuarioRepository cuentas;

  // ---------- docente ----------

  @Test
  void bloqueaAlDocenteConClaseEnOtraSeccionAlMismoBloqueYDia() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int h1 = horario(seccion(), plantilla, ANIO);
    int h2 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);

    sesion(h1, bloque, "MONDAY", area, docente, aula()).andExpect(status().isCreated());
    sesion(h2, bloque, "MONDAY", area, docente, aula())
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value(
                    containsString(
                        "El docente Marta Quispe ya tiene clase el lunes de 07:30 a 08:15")));
  }

  @Test
  void permiteAlMismoDocenteEnOtroDia() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int h1 = horario(seccion(), plantilla, ANIO);
    int h2 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);

    sesion(h1, bloque, "MONDAY", area, docente, aula()).andExpect(status().isCreated());
    sesion(h2, bloque, "TUESDAY", area, docente, aula()).andExpect(status().isCreated());
  }

  @Test
  void detectaCruceParcialEntreSeccionesConPlantillasDistintas() throws Exception {
    JsonNode a = plantilla("07:30:00", "13:00:00");
    JsonNode b = plantilla("08:00:00", "13:30:00");
    String area = area();
    int docente = docente("Marta");
    int h1 = horario(seccion(), a, ANIO);
    int h2 = horario(seccion(), b, ANIO);

    sesion(h1, bloqueId(a, 0), "MONDAY", area, docente, aula()).andExpect(status().isCreated());
    sesion(h2, bloqueId(b, 0), "MONDAY", area, docente, aula())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(containsString("ya tiene clase")));
  }

  @Test
  void noConsideraCruceLosBloquesQueSoloSeTocanEnUnExtremo() throws Exception {
    JsonNode a = plantilla("07:30:00", "13:00:00");
    JsonNode contigua = plantilla("08:15:00", "13:45:00");
    String area = area();
    int docente = docente("Marta");
    int h1 = horario(seccion(), a, ANIO);
    int h2 = horario(seccion(), contigua, ANIO);

    sesion(h1, bloqueId(a, 0), "MONDAY", area, docente, aula()).andExpect(status().isCreated());
    sesion(h2, bloqueId(contigua, 0), "MONDAY", area, docente, aula())
        .andExpect(status().isCreated());
  }

  @Test
  void noCruzaHorariosDeAniosEscolaresDistintos() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int h2027 = horario(seccion(), plantilla, 2027);
    int h2028 = horario(seccion(), plantilla, 2028);
    int bloque = bloqueId(plantilla, 0);

    sesion(h2027, bloque, "MONDAY", area, docente, aula()).andExpect(status().isCreated());
    sesion(h2028, bloque, "MONDAY", area, docente, aula()).andExpect(status().isCreated());
  }

  // ---------- aula ----------

  @Test
  void bloqueaElAulaOcupadaPorOtraSeccion() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int aula = aula();
    int h1 = horario(seccion(), plantilla, ANIO);
    int h2 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);

    sesion(h1, bloque, "WEDNESDAY", area, docente("Marta"), aula).andExpect(status().isCreated());
    sesion(h2, bloque, "WEDNESDAY", area, docente("Luis"), aula)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(containsString("ya está ocupada el miércoles")));
  }

  // ---------- edición ----------

  @Test
  void laEdicionNoChocaConLaPropiaSesion() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int aula = aula();
    int h1 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);
    int sesionId = idDe(sesion(h1, bloque, "MONDAY", area, docente, aula));

    mvc.perform(
            put(BASE + "/" + h1 + "/sesiones/" + sesionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(bloque, "MONDAY", area, docente, aula)))
        .andExpect(status().isOk());
  }

  @Test
  void laEdicionHaciaUnCruceSeRechaza() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int h1 = horario(seccion(), plantilla, ANIO);
    int h2 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);
    sesion(h1, bloque, "MONDAY", area, docente, aula()).andExpect(status().isCreated());
    int sesionId = idDe(sesion(h2, bloque, "TUESDAY", area, docente, aula()));

    mvc.perform(
            put(BASE + "/" + h2 + "/sesiones/" + sesionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(bloque, "MONDAY", area, docente, aula())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(containsString("ya tiene clase")));
  }

  // ---------- validación en tiempo real ----------

  @Test
  void validarInformaElCruceYNoGuardaNada() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int h1 = horario(seccion(), plantilla, ANIO);
    int h2 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);
    sesion(h1, bloque, "MONDAY", area, docente, aula()).andExpect(status().isCreated());

    validar(h2, bloque, "MONDAY", area, docente, aula(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valida").value(false))
        .andExpect(jsonPath("$.conflictos[0].tipo").value("DOCENTE"));

    mvc.perform(get(BASE + "/" + h2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(0));
  }

  @Test
  void validarConfirmaUnaAsignacionLibre() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    int h1 = horario(seccion(), plantilla, ANIO);
    validar(h1, bloqueId(plantilla, 0), "FRIDAY", area(), docente("Marta"), aula(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valida").value(true))
        .andExpect(jsonPath("$.conflictos.length()").value(0));
  }

  @Test
  void validarInformaDocenteYAulaALaVez() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int aula = aula();
    int h1 = horario(seccion(), plantilla, ANIO);
    int h2 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);
    sesion(h1, bloque, "THURSDAY", area, docente, aula).andExpect(status().isCreated());

    validar(h2, bloque, "THURSDAY", area, docente, aula, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valida").value(false))
        .andExpect(jsonPath("$.conflictos.length()").value(2))
        .andExpect(jsonPath("$.conflictos[0].tipo").value("DOCENTE"))
        .andExpect(jsonPath("$.conflictos[1].tipo").value("AULA"));
  }

  @Test
  void validarEdicionExcluyeLaPropiaSesion() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    String area = area();
    int docente = docente("Marta");
    int aula = aula();
    int h1 = horario(seccion(), plantilla, ANIO);
    int bloque = bloqueId(plantilla, 0);
    int sesionId = idDe(sesion(h1, bloque, "MONDAY", area, docente, aula));

    validar(h1, bloque, "MONDAY", area, docente, aula, sesionId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valida").value(true));
    validar(h1, bloque, "MONDAY", area, docente, aula, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valida").value(false))
        .andExpect(jsonPath("$.conflictos[0].tipo").value("CELDA"));
  }

  @Test
  void validarRechazaSesionAExcluirInexistente() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    int h1 = horario(seccion(), plantilla, ANIO);
    validar(h1, bloqueId(plantilla, 0), "MONDAY", area(), docente("Marta"), aula(), 999999)
        .andExpect(status().isNotFound());
  }

  @Test
  void validarRechazaUnRecreo() throws Exception {
    JsonNode plantilla = plantilla("07:30:00", "13:00:00");
    int h1 = horario(seccion(), plantilla, ANIO);
    validar(h1, bloqueId(plantilla, 2), "MONDAY", area(), docente("Marta"), aula(), null)
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithMockUser(username = "docente-test", roles = "DOCENTE")
  void validarRechazaUsuariosSinRolAdministrador() throws Exception {
    mvc.perform(
            post(BASE + "/1/sesiones/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(1, "MONDAY", "X", 1, 1)))
        .andExpect(status().isForbidden());
  }

  // ---------- apoyo ----------

  private JsonNode plantilla(String inicio, String fin) throws Exception {
    String cuerpo =
        """
        {"nombre":"Plantilla %s","turno":"MANANA","horaInicio":"%s","horaFin":"%s",
        "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":6,
        "recreos":[{"despuesDeBloque":2,"duracionMinutos":15},{"despuesDeBloque":4,"duracionMinutos":20}]}
        """
            .formatted(UUID.randomUUID().toString().substring(0, 8), inicio, fin);
    ResultActions creada =
        mvc.perform(
                post("/api/v1/horarios/plantillas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
            .andExpect(status().isCreated());
    return json.readTree(creada.andReturn().getResponse().getContentAsString());
  }

  private int bloqueId(JsonNode plantilla, int indice) {
    return plantilla.get("bloques").get(indice).get("id").asInt();
  }

  private int seccion() {
    String s = UUID.randomUUID().toString().substring(0, 8);
    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(1, NivelEducativo.SECUNDARIA)
            .orElseThrow();
    Seccion seccion = new Seccion();
    seccion.setCodigo("T" + s);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    return secciones.save(seccion).getId();
  }

  private String area() {
    String s = UUID.randomUUID().toString().substring(0, 8);
    Area area = new Area();
    area.setCodigo("TST-" + s);
    area.setNombre("Área de prueba " + s);
    return areas.save(area).getCodigo();
  }

  private int aula() {
    String s = UUID.randomUUID().toString().substring(0, 8);
    Aula aula = new Aula();
    aula.setCodigo("T-" + s);
    aula.setCapacidad(30);
    return aulas.save(aula).getId();
  }

  private int docente(String nombre) {
    String s = UUID.randomUUID().toString().substring(0, 8);
    Persona persona = personas.save(new Persona("2" + s, nombre, "Quispe", "Salas"));
    return cuentas.save(new CuentaUsuario("docente-" + s, "hash", Rol.DOCENTE, persona)).getId();
  }

  private int horario(int seccionId, JsonNode plantilla, int anio) throws Exception {
    ResultActions creado =
        mvc.perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "seccionId",
                                seccionId,
                                "anio",
                                anio,
                                "plantillaId",
                                plantilla.get("id").asInt()))))
            .andExpect(status().isCreated());
    return idDe(creado);
  }

  private ResultActions sesion(
      int horarioId, int bloqueId, String dia, String area, int docenteId, int aulaId)
      throws Exception {
    return mvc.perform(
        post(BASE + "/" + horarioId + "/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(bloqueId, dia, area, docenteId, aulaId)));
  }

  private ResultActions validar(
      int horarioId,
      int bloqueId,
      String dia,
      String area,
      int docenteId,
      int aulaId,
      Integer excluirSesionId)
      throws Exception {
    var peticion =
        post(BASE + "/" + horarioId + "/sesiones/validar")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(bloqueId, dia, area, docenteId, aulaId));
    if (excluirSesionId != null) {
      peticion = peticion.param("excluirSesionId", String.valueOf(excluirSesionId));
    }
    return mvc.perform(peticion);
  }

  private String cuerpo(int bloqueId, String dia, String area, int docenteId, int aulaId)
      throws Exception {
    return json.writeValueAsString(
        Map.of(
            "bloqueId", bloqueId,
            "dia", dia,
            "areaCodigo", area,
            "docenteId", docenteId,
            "aulaId", aulaId));
  }

  private int idDe(ResultActions respuesta) throws Exception {
    respuesta.andExpect(status().is2xxSuccessful());
    return json.readTree(respuesta.andReturn().getResponse().getContentAsString())
        .get("id")
        .asInt();
  }
}
