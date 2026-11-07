package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import pe.edu.pucp.colegio.common.model.PlantillaHorario;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.PlantillaHorarioRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "horario-seccion-test", roles = "ADMINISTRADOR")
@Transactional
class HorarioSeccionIntegrationTest {

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
  @Autowired PlantillaHorarioRepository plantillas;

  /** Datos mínimos que necesita un horario: sección, plantilla, área, docente y aula. */
  record Escenario(
      int seccionId,
      int plantillaId,
      int bloque1Id,
      int bloque2Id,
      int recreoId,
      String areaCodigo,
      int docenteId,
      int aulaId,
      String sufijo) {}

  @Test
  void creaHorarioYDevuelveGrillaConBloquesYRecreos() throws Exception {
    Escenario e = escenario();
    crearHorario(e)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.anio").value(ANIO))
        .andExpect(jsonPath("$.turno").value("MANANA"))
        .andExpect(jsonPath("$.dias.length()").value(5))
        .andExpect(jsonPath("$.dias[0]").value("MONDAY"))
        .andExpect(jsonPath("$.dias[4]").value("FRIDAY"))
        .andExpect(jsonPath("$.bloques.length()").value(8))
        .andExpect(jsonPath("$.bloques[2].tipo").value("RECREO"))
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(0));
  }

  @Test
  void rechazaSegundoHorarioParaLaMismaSeccionYAnio() throws Exception {
    Escenario e = escenario();
    crearHorario(e).andExpect(status().isCreated());
    crearHorario(e).andExpect(status().isConflict());
  }

  @Test
  void rechazaSeccionInexistente() throws Exception {
    Escenario e = escenario();
    mvc.perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cuerpo(
                        Map.of(
                            "seccionId", 999999, "anio", ANIO, "plantillaId", e.plantillaId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void rechazaPlantillaInactiva() throws Exception {
    Escenario e = escenario();
    PlantillaHorario plantilla = plantillas.findById(e.plantillaId()).orElseThrow();
    plantilla.setActive(false);
    plantillas.save(plantilla);
    crearHorario(e).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void consultaHorarioPorSeccionYAnio() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    mvc.perform(
            get(BASE).param("seccionId", String.valueOf(e.seccionId())).param("anio", "" + ANIO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(horarioId));
    mvc.perform(
            get(BASE).param("seccionId", String.valueOf(e.seccionId())).param("anio", "2099"))
        .andExpect(status().isNotFound());
  }

  @Test
  void asignaSesionYApareceEnLaGrilla() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.dia").value("MONDAY"))
        .andExpect(jsonPath("$.areaCodigo").value(e.areaCodigo()))
        .andExpect(jsonPath("$.aulaId").value(e.aulaId()));

    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(1))
        .andExpect(jsonPath("$.bloques[0].sesiones[0].dia").value("MONDAY"))
        .andExpect(jsonPath("$.bloques[0].sesiones[0].docenteNombre").value("Marta Quispe"))
        .andExpect(jsonPath("$.bloques[1].sesiones.length()").value(0));
  }

  @Test
  void rechazaCeldaYaOcupada() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "TUESDAY").andExpect(status().isCreated());
    agregarSesion(horarioId, e, e.bloque1Id(), "TUESDAY").andExpect(status().isConflict());
  }

  @Test
  void rechazaSesionEnRecreo() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.recreoId(), "MONDAY").andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaSabadoYDomingo() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "SATURDAY")
        .andExpect(status().isUnprocessableEntity());
    agregarSesion(horarioId, e, e.bloque1Id(), "SUNDAY")
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaDiaInexistente() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "FUNDAY").andExpect(status().isBadRequest());
  }

  @Test
  void rechazaBloqueDeOtraPlantilla() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    JsonNode otra = crearPlantilla(UUID.randomUUID().toString().substring(0, 8));
    int bloqueAjeno = otra.get("bloques").get(0).get("id").asInt();
    agregarSesion(horarioId, e, bloqueAjeno, "MONDAY").andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaDocenteQueNoTieneRolDocente() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    Persona persona = personas.save(new Persona("3" + e.sufijo(), "Luis", "Rojas", "Luna"));
    CuentaUsuario alumno =
        cuentas.save(new CuentaUsuario("alumno-" + e.sufijo(), "hash", Rol.ALUMNO, persona));
    mvc.perform(
            post(BASE + "/" + horarioId + "/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesion(e, e.bloque1Id(), "MONDAY", alumno.getId(), e.aulaId())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaAulaInactiva() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    Aula aula = aulas.findById(e.aulaId()).orElseThrow();
    aula.setActive(false);
    aulas.save(aula);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isUnprocessableEntity());
  }

  @Test
  void actualizaElDiaDeUnaSesion() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    mvc.perform(
            put(BASE + "/" + horarioId + "/sesiones/" + sesionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesion(e, e.bloque1Id(), "WEDNESDAY", e.docenteId(), e.aulaId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dia").value("WEDNESDAY"));
  }

  @Test
  void eliminaUnaSesion() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque2Id(), "FRIDAY"));
    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNoContent());
    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloques[1].sesiones.length()").value(0));
    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "docente-test", roles = "DOCENTE")
  void rechazaUsuariosSinRolAdministrador() throws Exception {
    mvc.perform(get(BASE + "/1")).andExpect(status().isForbidden());
  }

  // ---------- apoyo ----------

  private Escenario escenario() throws Exception {
    String s = UUID.randomUUID().toString().substring(0, 8);
    JsonNode plantilla = crearPlantilla(s);

    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(1, NivelEducativo.SECUNDARIA)
            .orElseThrow();
    Seccion seccion = new Seccion();
    seccion.setCodigo("T" + s);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    seccion = secciones.save(seccion);

    Area area = new Area();
    area.setCodigo("TST-" + s);
    area.setNombre("Área de prueba " + s);
    area = areas.save(area);

    Aula aula = new Aula();
    aula.setCodigo("T-" + s);
    aula.setCapacidad(30);
    aula = aulas.save(aula);

    Persona persona = personas.save(new Persona("2" + s, "Marta", "Quispe", "Salas"));
    CuentaUsuario docente =
        cuentas.save(new CuentaUsuario("docente-" + s, "hash", Rol.DOCENTE, persona));

    JsonNode bloques = plantilla.get("bloques");
    return new Escenario(
        seccion.getId(),
        plantilla.get("id").asInt(),
        bloques.get(0).get("id").asInt(),
        bloques.get(1).get("id").asInt(),
        bloques.get(2).get("id").asInt(),
        area.getCodigo(),
        docente.getId(),
        aula.getId(),
        s);
  }

  private JsonNode crearPlantilla(String sufijo) throws Exception {
    String cuerpo =
        """
        {"nombre":"Plantilla %s","turno":"MANANA","horaInicio":"07:30:00","horaFin":"13:00:00",
        "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":6,
        "recreos":[{"despuesDeBloque":2,"duracionMinutos":15},{"despuesDeBloque":4,"duracionMinutos":20}]}
        """
            .formatted(sufijo);
    ResultActions creada =
        mvc.perform(
                post("/api/v1/horarios/plantillas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
            .andExpect(status().isCreated());
    return json.readTree(creada.andReturn().getResponse().getContentAsString());
  }

  private ResultActions crearHorario(Escenario e) throws Exception {
    return mvc.perform(
        post(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                cuerpo(
                    Map.of(
                        "seccionId", e.seccionId(), "anio", ANIO, "plantillaId", e.plantillaId()))));
  }

  private ResultActions agregarSesion(int horarioId, Escenario e, int bloqueId, String dia)
      throws Exception {
    return mvc.perform(
        post(BASE + "/" + horarioId + "/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(sesion(e, bloqueId, dia, e.docenteId(), e.aulaId())));
  }

  private String sesion(Escenario e, int bloqueId, String dia, int docenteId, int aulaId)
      throws Exception {
    return cuerpo(
        Map.of(
            "bloqueId", bloqueId,
            "dia", dia,
            "areaCodigo", e.areaCodigo(),
            "docenteId", docenteId,
            "aulaId", aulaId));
  }

  private String cuerpo(Object datos) throws Exception {
    return json.writeValueAsString(datos);
  }

  private int idDe(ResultActions respuesta) throws Exception {
    String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
    return json.readTree(cuerpo).get("id").asInt();
  }
}
