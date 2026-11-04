package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "docente-seguimiento-test", roles = "DOCENTE")
@TestPropertySource(properties = "app.storage.evidence-dir=target/test-evidencias")
@Transactional
class SeguimientoAcademicoIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @PersistenceContext EntityManager entityManager;

  private Integer periodoId;
  private Integer matriculaId;
  private String competenciaCodigo;

  @BeforeEach
  void prepararDatos() {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    Grado grado = new Grado();
    grado.setNumero(Math.abs(sufijo.hashCode()) + 1);
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);

    Seccion seccion = new Seccion();
    seccion.setCodigo("A-" + sufijo);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    entityManager.flush();

    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(LocalDate.of(2026, 3, 1));
    periodo.setFechaFin(LocalDate.of(2026, 12, 20));
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    entityManager.persist(periodo);

    Competencia competencia = new Competencia();
    competenciaCodigo = "MAT-" + sufijo;
    competencia.setCodigo(competenciaCodigo);
    competencia.setNombre("Resuelve problemas matemáticos " + sufijo);
    entityManager.persist(competencia);

    Estudiante estudiante = nuevoEstudiante(sufijo, "Ana", "Pérez");
    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(LocalDate.of(2026, 3, 2));
    matricula.setEstado(EstadoMatricula.CONFIRMADA);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);

    entityManager.flush();
    periodoId = periodo.getId();
    matriculaId = matricula.getId();
  }

  @Test
  void creaTareaYPermiteMarcarEntregaDeUnAlumno() throws Exception {
    String solicitud = """
        {"periodoId":%d,"competenciaCodigo":"%s","titulo":"Práctica de fracciones",
        "descripcion":"Resolver los ejercicios asignados.","fechaLimite":"2026-11-20T23:59:00","ponderacion":20.00}
        """.formatted(periodoId, competenciaCodigo);

    MvcResult resultado = mvc.perform(post("/api/v1/tareas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(solicitud))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titulo").value("Práctica de fracciones"))
        .andExpect(jsonPath("$.ponderacion").value(20.0))
        .andExpect(jsonPath("$.entregas.length()").value(1))
        .andExpect(jsonPath("$.entregas[0].estudiante").value("Ana Pérez Pérez"))
        .andExpect(jsonPath("$.entregas[0].cumplida").value(false))
        .andReturn();
    int tareaId = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asInt();

    mvc.perform(patch("/api/v1/tareas/{tareaId}/entregas/{matriculaId}", tareaId, matriculaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cumplida\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entregas[0].cumplida").value(true))
        .andExpect(jsonPath("$.entregas[0].fechaCumplimiento").isNotEmpty());

    mvc.perform(get("/api/v1/tareas").param("periodoId", periodoId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(tareaId))
        .andExpect(jsonPath("$[0].entregas[0].cumplida").value(true));
  }

  @Test
  void registraCalificacionConUrlYLaDevuelveEnElSeguimientoDeCompetencia() throws Exception {
    CalificacionRequestBody datos = new CalificacionRequestBody(
        periodoId, matriculaId, competenciaCodigo, "AD", "Resolución completa", "https://example.org/evidencia");

    mvc.perform(multipart("/api/v1/calificaciones")
            .part(partJson("datos", objectMapper.writeValueAsBytes(datos))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nota").value("AD"))
        .andExpect(jsonPath("$.evidenciaTipo").value("URL"))
        .andExpect(jsonPath("$.evidenciaUrl").value("https://example.org/evidencia"));

    mvc.perform(get("/api/v1/seguimiento/competencias/{codigo}", competenciaCodigo)
            .param("periodoId", periodoId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].estudiante").value("Ana Pérez Pérez"))
        .andExpect(jsonPath("$[0].nota").value("AD"))
        .andExpect(jsonPath("$[0].evidenciaUrl").value("https://example.org/evidencia"));
  }

  @Test
  void adjuntaArchivoYPermiteRecuperarloDesdeLaFichaDeSeguimiento() throws Exception {
    CalificacionRequestBody datos = new CalificacionRequestBody(
        periodoId, matriculaId, competenciaCodigo, "A", "Trabajo revisado", null);
    MockMultipartFile archivo = new MockMultipartFile("archivo", "trabajo.txt", "text/plain", "contenido de evidencia".getBytes());

    MvcResult resultado = mvc.perform(multipart("/api/v1/calificaciones")
            .file(archivo)
            .part(partJson("datos", objectMapper.writeValueAsBytes(datos))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nota").value("A"))
        .andExpect(jsonPath("$.evidenciaTipo").value("ARCHIVO"))
        .andExpect(jsonPath("$.evidenciaNombre").value("trabajo.txt"))
        .andReturn();
    JsonNode respuesta = objectMapper.readTree(resultado.getResponse().getContentAsString());
    String urlArchivo = respuesta.get("evidenciaUrl").asText();

    mvc.perform(get(urlArchivo))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
            .bytes("contenido de evidencia".getBytes()));
  }

  @Test
  void rechazaPonderacionFueraDelRangoPermitido() throws Exception {
    String solicitud = """
        {"periodoId":%d,"competenciaCodigo":"%s","titulo":"Práctica",
        "descripcion":"Descripción","fechaLimite":"2026-11-20T23:59:00","ponderacion":101}
        """.formatted(periodoId, competenciaCodigo);

    mvc.perform(post("/api/v1/tareas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(solicitud))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rechazaUrlDeEvidenciaQueNoSeaHttpOHttps() throws Exception {
    CalificacionRequestBody datos = new CalificacionRequestBody(
        periodoId, matriculaId, competenciaCodigo, "B", null, "ftp://example.org/evidencia");

    mvc.perform(multipart("/api/v1/calificaciones")
            .part(partJson("datos", objectMapper.writeValueAsBytes(datos))))
        .andExpect(status().isUnprocessableEntity());
  }

  private Estudiante nuevoEstudiante(String sufijo, String nombre, String apellido) {
    Persona persona = new Persona("T" + sufijo, nombre, apellido, apellido);
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);
    return estudiante;
  }

  private MockPart partJson(String nombre, byte[] contenido) {
    MockPart parte = new MockPart(nombre, contenido);
    parte.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return parte;
  }

  private record CalificacionRequestBody(
      Integer periodoId,
      Integer matriculaId,
      String competenciaCodigo,
      String nota,
      String observacion,
      String evidenciaUrl) {}
}
