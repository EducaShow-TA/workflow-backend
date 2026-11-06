package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.RecalculoLogrosRequest;
import pe.edu.pucp.colegio.common.model.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin-consolidacion-test", roles = "ADMINISTRADOR")
@TestPropertySource(properties = "app.storage.evidence-dir=target/test-evidencias")
@Transactional
class ConsolidacionLogroIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @PersistenceContext EntityManager entityManager;

  private Integer periodoId;
  private Integer gradoId;
  private Integer seccionId;
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
    gradoId = grado.getId();
    seccionId = seccion.getId();

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
    competencia.setNombre("Competencia " + sufijo);
    entityManager.persist(competencia);

    Persona persona = new Persona("T" + sufijo, "Ana", "Pérez", "López");
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);

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
  void calculaEnLineaConPesosYUmbralesConfigurables() throws Exception {
    int primera = crearEvaluacion("75.00");
    int segunda = crearEvaluacion("25.00");
    mvc.perform(
            multipart("/api/v1/calificaciones")
                .part(
                    parteJson(
                        "datos",
                        objectMapper.writeValueAsBytes(
                            new CalificacionManualRequest(
                                periodoId,
                                matriculaId,
                                competenciaCodigo,
                                "AD",
                                null,
                                "https://example.org/evidencia")))))
        .andExpect(status().isUnprocessableEntity());
    guardarNota(primera, "A", false);

    consultarLogro()
        .andExpect(jsonPath("$[0].nota").value("A"))
        .andExpect(jsonPath("$[0].puntajeConsolidado").value(3.0))
        .andExpect(jsonPath("$[0].consolidada").value(true));

    guardarNota(segunda, "C", false);
    consultarLogro()
        .andExpect(jsonPath("$[0].nota").value("A"))
        .andExpect(jsonPath("$[0].puntajeConsolidado").value(2.5));

    mvc.perform(
            post("/api/v1/logros/recalculo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RecalculoLogrosRequest(periodoId, null, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodosProcesados").value(1))
        .andExpect(jsonPath("$.logrosActualizados").value(1));

    mvc.perform(
            post("/api/v1/logros/recalculo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RecalculoLogrosRequest(null, gradoId, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodosProcesados").value(1))
        .andExpect(jsonPath("$.logrosActualizados").value(1));

    mvc.perform(
            post("/api/v1/logros/recalculo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RecalculoLogrosRequest(null, null, seccionId))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodosProcesados").value(1))
        .andExpect(jsonPath("$.logrosActualizados").value(1));

    mvc.perform(
            put("/api/v1/competencias/{codigo}/regla-consolidacion", competenciaCodigo)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"umbralB":1.60,"umbralA":2.80,"umbralAd":3.60}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.umbralA").value(2.8));

    consultarLogro()
        .andExpect(jsonPath("$[0].nota").value("B"))
        .andExpect(jsonPath("$[0].puntajeConsolidado").value(2.5));
  }

  @Test
  void cierreConvierteFaltantesEnCYEImpideCambiosPosteriores() throws Exception {
    int primera = crearEvaluacion("40.00");
    int segunda = crearEvaluacion("60.00");
    int justificada = crearEvaluacion("10.00");
    guardarNota(primera, "A", false);
    guardarNota(justificada, null, true);

    consultarLogro()
        .andExpect(jsonPath("$[0].nota").value("A"))
        .andExpect(jsonPath("$[0].puntajeConsolidado").value(3.0));

    mvc.perform(post("/api/v1/periodos/{id}/cierre-actas", periodoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.actasCerradas").value(true))
        .andExpect(jsonPath("$.usuario").value("admin-consolidacion-test"));

    mvc.perform(get("/api/v1/periodos/{id}/cierre-actas", periodoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.actasCerradas").value(true));

    consultarLogro()
        .andExpect(jsonPath("$[0].nota").value("B"))
        .andExpect(jsonPath("$[0].puntajeConsolidado").value(1.8));

    mvc.perform(get("/api/v1/evaluaciones-parciales/{id}/calificaciones", segunda))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nota").value("C"))
        .andExpect(jsonPath("$[0].justificada").value(false));

    mvc.perform(get("/api/v1/evaluaciones-parciales/{id}/calificaciones", justificada))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nota").value(nullValue()))
        .andExpect(jsonPath("$[0].justificada").value(true));

    mvc.perform(
            put(
                    "/api/v1/evaluaciones-parciales/{evaluacionId}/matriculas/{matriculaId}",
                    primera,
                    matriculaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nota":"AD","justificada":false}
                    """))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void permiteEvidenciaAdjuntaEnUnaCalificacionParcial() throws Exception {
    int evaluacionId = crearEvaluacion("100.00");
    MockPart datos =
        new MockPart(
            "datos",
            """
            {"nota":"AD","justificada":false}
            """
                .getBytes());
    datos.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "evidencia.txt", "text/plain", "evidencia".getBytes());

    MvcResult resultado =
        mvc.perform(
                multipart(
                        HttpMethod.PUT,
                        "/api/v1/evaluaciones-parciales/{evaluacionId}/matriculas/{matriculaId}",
                        evaluacionId,
                        matriculaId)
                    .part(datos)
                    .file(archivo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evidenciaTipo").value("ARCHIVO"))
            .andExpect(jsonPath("$.evidenciaNombre").value("evidencia.txt"))
            .andReturn();
    String url =
        objectMapper
            .readTree(resultado.getResponse().getContentAsString())
            .get("evidenciaUrl")
            .asText();

    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .bytes("evidencia".getBytes()));
  }

  private int crearEvaluacion(String ponderacion) throws Exception {
    String solicitud =
        """
        {"periodoId":%d,"competenciaCodigo":"%s","titulo":"Evaluación parcial",
        "descripcion":"Evidencia de aprendizaje","ponderacion":%s}
        """
            .formatted(periodoId, competenciaCodigo, ponderacion);
    MvcResult resultado =
        mvc.perform(
                post("/api/v1/evaluaciones-parciales")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(solicitud))
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode respuesta = objectMapper.readTree(resultado.getResponse().getContentAsString());
    return respuesta.get("id").asInt();
  }

  private void guardarNota(int evaluacionId, String nota, boolean justificada) throws Exception {
    mvc.perform(
            put(
                    "/api/v1/evaluaciones-parciales/{evaluacionId}/matriculas/{matriculaId}",
                    evaluacionId,
                    matriculaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CalificacionParcialRequestBody(nota, justificada, null))))
        .andExpect(status().isOk());
  }

  private org.springframework.test.web.servlet.ResultActions consultarLogro() throws Exception {
    return mvc.perform(
            get("/api/v1/seguimiento/competencias/{codigo}", competenciaCodigo)
                .param("periodoId", periodoId.toString()))
        .andExpect(status().isOk());
  }

  private MockPart parteJson(String nombre, byte[] contenido) {
    MockPart parte = new MockPart(nombre, contenido);
    parte.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return parte;
  }

  private record CalificacionManualRequest(
      Integer periodoId,
      Integer matriculaId,
      String competenciaCodigo,
      String nota,
      String observacion,
      String evidenciaUrl) {}

  private record CalificacionParcialRequestBody(
      String nota, boolean justificada, String evidenciaUrl) {}
}
