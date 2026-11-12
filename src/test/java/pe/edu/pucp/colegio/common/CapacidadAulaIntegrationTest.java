package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
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
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
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

/** US-4.4: el aula debe tener capacidad para los alumnos de la sección. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "capacidad-aula-test", roles = "ADMINISTRADOR")
@Transactional
class CapacidadAulaIntegrationTest {

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
  @PersistenceContext EntityManager entityManager;

  /** Sección con un horario creado y N alumnos matriculados en el año del horario. */
  record Caso(int horarioId, int bloqueId, String areaCodigo, int docenteId, Seccion seccion) {}

  @Test
  void rechazaAulaConMenosCapacidadQueLosAlumnosDeLaSeccion() throws Exception {
    Caso caso = caso(3);
    int aula = aula(2);
    sesion(caso, aula)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("capacidad para 2 alumnos")))
        .andExpect(jsonPath("$.message").value(containsString("tiene 3 matriculados en 2027")));
  }

  @Test
  void permiteAulaConCapacidadIgualAlTotalDeAlumnos() throws Exception {
    Caso caso = caso(3);
    sesion(caso, aula(3)).andExpect(status().isCreated());
  }

  @Test
  void permiteAulaConCapacidadSobrada() throws Exception {
    Caso caso = caso(3);
    sesion(caso, aula(30)).andExpect(status().isCreated());
  }

  @Test
  void noCuentaMatriculasInactivasRetiradasNiDeOtroAnio() throws Exception {
    Caso caso = caso(2);
    Periodo periodo = periodo(caso.seccion(), LocalDate.of(ANIO, 1, 10), LocalDate.of(ANIO, 2, 20));
    matricula(periodo, EstadoMatricula.CONFIRMADA, false);
    matricula(periodo, EstadoMatricula.RETIRADA, true);
    Periodo anterior =
        periodo(caso.seccion(), LocalDate.of(ANIO - 1, 3, 1), LocalDate.of(ANIO - 1, 12, 15));
    matricula(anterior, EstadoMatricula.CONFIRMADA, true);

    sesion(caso, aula(2)).andExpect(status().isCreated());
  }

  @Test
  void validarInformaLaCapacidadComoConflicto() throws Exception {
    Caso caso = caso(3);
    int aula = aula(2);
    mvc.perform(
            post(BASE + "/" + caso.horarioId() + "/sesiones/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(caso, aula)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valida").value(false))
        .andExpect(jsonPath("$.conflictos.length()").value(1))
        .andExpect(jsonPath("$.conflictos[0].tipo").value("CAPACIDAD"));
  }

  @Test
  void noPermiteReducirLaCapacidadPorDebajoDeLosAlumnosDeUnaSeccionAsignada() throws Exception {
    Caso caso = caso(3);
    int aulaId = aula(5);
    sesion(caso, aulaId).andExpect(status().isCreated());
    String codigo = aulas.findById(aulaId).orElseThrow().getCodigo();

    actualizarAula(aulaId, codigo, 2)
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message").value(containsString("No se puede reducir la capacidad a 2")));
    actualizarAula(aulaId, codigo, 3)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capacidad").value(3));
  }

  @Test
  void permiteReducirLaCapacidadDeUnAulaSinClases() throws Exception {
    caso(3);
    int aulaId = aula(40);
    String codigo = aulas.findById(aulaId).orElseThrow().getCodigo();
    actualizarAula(aulaId, codigo, 1).andExpect(status().isOk());
  }

  // ---------- apoyo ----------

  private Caso caso(int alumnos) throws Exception {
    JsonNode plantilla = plantilla();
    Seccion seccion = seccion();

    Periodo periodo = periodo(seccion, LocalDate.of(ANIO, 3, 1), LocalDate.of(ANIO, 12, 15));
    for (int i = 0; i < alumnos; i++) {
      matricula(
          periodo, i % 2 == 0 ? EstadoMatricula.CONFIRMADA : EstadoMatricula.EN_REGISTRO, true);
    }

    ResultActions creado =
        mvc.perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "seccionId",
                                seccion.getId(),
                                "anio",
                                ANIO,
                                "plantillaId",
                                plantilla.get("id").asInt()))))
            .andExpect(status().isCreated());
    int horarioId =
        json.readTree(creado.andReturn().getResponse().getContentAsString()).get("id").asInt();

    String s = UUID.randomUUID().toString().substring(0, 8);
    Area area = new Area();
    area.setCodigo("TST-" + s);
    area.setNombre("Área de prueba " + s);
    area = areas.save(area);

    Persona persona = personas.save(new Persona("2" + s, "Marta", "Quispe", "Salas"));
    CuentaUsuario docente =
        cuentas.save(new CuentaUsuario("docente-" + s, "hash", Rol.DOCENTE, persona));

    return new Caso(
        horarioId,
        plantilla.get("bloques").get(0).get("id").asInt(),
        area.getCodigo(),
        docente.getId(),
        seccion);
  }

  private JsonNode plantilla() throws Exception {
    String cuerpo =
        """
        {"nombre":"Plantilla %s","turno":"MANANA","horaInicio":"07:30:00","horaFin":"13:00:00",
        "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":6,
        "recreos":[{"despuesDeBloque":2,"duracionMinutos":15},{"despuesDeBloque":4,"duracionMinutos":20}]}
        """
            .formatted(UUID.randomUUID().toString().substring(0, 8));
    ResultActions creada =
        mvc.perform(
                post("/api/v1/horarios/plantillas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
            .andExpect(status().isCreated());
    return json.readTree(creada.andReturn().getResponse().getContentAsString());
  }

  private Seccion seccion() {
    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(1, NivelEducativo.SECUNDARIA)
            .orElseThrow();
    Seccion seccion = new Seccion();
    seccion.setCodigo("T" + UUID.randomUUID().toString().substring(0, 8));
    seccion.setVacantesTotales(40);
    grado.agregarSeccion(seccion);
    return secciones.save(seccion);
  }

  private Periodo periodo(Seccion seccion, LocalDate inicio, LocalDate fin) {
    Periodo periodo = new Periodo();
    periodo.setGrado(seccion.getGrado());
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(inicio);
    periodo.setFechaFin(fin);
    periodo.setEstado(EstadoPeriodo.PLANIFICADO);
    entityManager.persist(periodo);
    return periodo;
  }

  private void matricula(Periodo periodo, EstadoMatricula estado, boolean activa) {
    String dni = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    Persona persona = new Persona(dni, "Estudiante", "Prueba", "Capacidad");
    entityManager.persist(persona);

    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);

    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(periodo.getFechaInicio());
    matricula.setEstado(estado);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    matricula.setActive(activa);
    entityManager.persist(matricula);
  }

  private int aula(int capacidad) {
    Aula aula = new Aula();
    aula.setCodigo("T-" + UUID.randomUUID().toString().substring(0, 8));
    aula.setCapacidad(capacidad);
    return aulas.save(aula).getId();
  }

  private ResultActions sesion(Caso caso, int aulaId) throws Exception {
    return mvc.perform(
        post(BASE + "/" + caso.horarioId() + "/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(caso, aulaId)));
  }

  private String cuerpo(Caso caso, int aulaId) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "bloqueId", caso.bloqueId(),
            "dia", "MONDAY",
            "areaCodigo", caso.areaCodigo(),
            "docenteId", caso.docenteId(),
            "aulaId", aulaId));
  }

  private ResultActions actualizarAula(int id, String codigo, int capacidad) throws Exception {
    return mvc.perform(
        put("/api/v1/aulas/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("codigo", codigo, "capacidad", capacidad))));
  }
}
