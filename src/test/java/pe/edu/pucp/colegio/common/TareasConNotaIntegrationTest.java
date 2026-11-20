package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.fixture.HorarioFixture;
import pe.edu.pucp.colegio.common.model.*;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.storage.evidence-dir=target/test-evidencias")
class TareasConNotaIntegrationTest {

  private static final String RUTA_ALUMNO = "/api/v1/estudiantes/mis-tareas";

  @Autowired MockMvc mvc;
  @Autowired Clock clock;
  @Autowired ObjectMapper objectMapper;
  @PersistenceContext EntityManager entityManager;

  private String sufijo;
  private String alumno;
  private String docente;
  private String administrador;
  private Integer matriculaId;
  private Periodo periodo;
  private Competencia competencia;
  private LocalDateTime ahora;

  @BeforeEach
  void prepararDatos() {
    sufijo = UUID.randomUUID().toString().substring(0, 8);
    ahora = LocalDateTime.now(clock);
    int anio = ahora.getYear();

    Grado grado = new Grado();
    grado.setNumero(Math.abs(sufijo.hashCode()) + 1);
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccion = new Seccion();
    seccion.setCodigo("N-" + sufijo);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(LocalDate.of(anio, 1, 1));
    periodo.setFechaFin(LocalDate.of(anio, 12, 31));
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    entityManager.persist(periodo);

    competencia = new Competencia();
    competencia.setCodigo("ESC-" + sufijo);
    competencia.setNombre("Escribe diversos tipos de textos " + sufijo);
    entityManager.persist(competencia);
    Area area = new Area();
    area.setCodigo("COM-" + sufijo);
    area.setNombre("Comunicación " + sufijo);
    area.getCompetencias().add(competencia);
    entityManager.persist(area);

    Persona personaDocente = new Persona("D" + sufijo, "Rosa", "Rojas", "León");
    entityManager.persist(personaDocente);
    docente = "docente-" + sufijo;
    entityManager.persist(new CuentaUsuario(docente, "hash", Rol.DOCENTE, personaDocente));
    Docente registroDocente = new Docente();
    registroDocente.setPersona(personaDocente);
    entityManager.persist(registroDocente);
    HorarioFixture.asignar(
        entityManager,
        seccion,
        area,
        docente,
        LocalDate.of(anio, 6, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)),
        LocalTime.of(8, 0),
        LocalTime.of(8, 45));
    administrador = "admin-" + sufijo;

    Persona personaAlumno = new Persona("E" + sufijo, "Lucía", "Ramos", "Díaz");
    entityManager.persist(personaAlumno);
    alumno = "alumno-" + sufijo;
    entityManager.persist(new CuentaUsuario(alumno, "hash", Rol.ALUMNO, personaAlumno));
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(personaAlumno);
    entityManager.persist(estudiante);
    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(periodo.getFechaInicio());
    matricula.setEstado(EstadoMatricula.CONFIRMADA);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);
    matriculaId = matricula.getId();
    entityManager.flush();
  }

  @Test
  void laTareaVinculadaTomaLaPonderacionYElBimestreDeLaEvaluacionDelPlan() throws Exception {
    EvaluacionParcial evaluacion = evaluacion("Ensayo argumentativo", "20.00", 4);
    entityManager.flush();

    crearTarea("Ensayo: el valor de la amistad", "5.00", evaluacion.getId())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ponderacion").value(20.0))
        .andExpect(jsonPath("$.evaluacionParcialId").value(evaluacion.getId()))
        .andExpect(jsonPath("$.evaluacionTitulo").value("Ensayo argumentativo"))
        .andExpect(jsonPath("$.bimestre").value(4));

    crearTarea("Otra tarea", null, evaluacion.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("ya está vinculada")));

    mvc.perform(
            get("/api/v1/evaluaciones-parciales")
                .param("periodoId", periodo.getId().toString())
                .param("competenciaCodigo", competencia.getCodigo())
                .with(user(docente).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tareaTitulo").value("Ensayo: el valor de la amistad"));
  }

  @Test
  void rechazaVincularUnaEvaluacionDeOtraCompetencia() throws Exception {
    Competencia otra = new Competencia();
    otra.setCodigo("LEE-" + sufijo);
    otra.setNombre("Lee diversos tipos de textos " + sufijo);
    entityManager.persist(otra);
    EvaluacionParcial ajena = new EvaluacionParcial();
    ajena.setPeriodo(periodo);
    ajena.setCompetencia(otra);
    ajena.setTitulo("Lectura");
    ajena.setPonderacion(new BigDecimal("30.00"));
    ajena.setBimestre(4);
    entityManager.persist(ajena);
    entityManager.flush();

    crearTarea("Ensayo", null, ajena.getId())
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("misma sección y competencia")));
  }

  @Test
  void elDocenteCalificaLaEntregaConComentarioYElAlumnoLaVeSinPoderReemplazarla() throws Exception {
    EvaluacionParcial evaluacion = evaluacion("Ensayo argumentativo", "20.00", 4);
    entityManager.flush();
    int tareaId = idDe(crearTarea("Ensayo", null, evaluacion.getId()));

    mvc.perform(entrega(tareaId, "ensayo.pdf")).andExpect(status().isOk());

    calificar(tareaId, "A", false, null, "Buena argumentación.")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entregas[0].nota").value("A"))
        .andExpect(jsonPath("$.entregas[0].comentario").value("Buena argumentación."))
        .andExpect(jsonPath("$.entregas[0].fechaCalificacion").isNotEmpty());

    mvc.perform(
            get("/api/v1/evaluaciones-parciales/{id}/calificaciones", evaluacion.getId())
                .with(user(docente).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nota").value("A"))
        .andExpect(jsonPath("$[0].evidenciaTipo").value("ARCHIVO"))
        .andExpect(jsonPath("$[0].evidenciaNombre").value("ensayo.pdf"))
        .andExpect(jsonPath("$[0].comentario").value("Buena argumentación."));

    mvc.perform(get(RUTA_ALUMNO + "/{id}", tareaId).with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cuentaParaNota").value(true))
        .andExpect(jsonPath("$.bimestre").value(4))
        .andExpect(jsonPath("$.nota").value("A"))
        .andExpect(jsonPath("$.comentario").value("Buena argumentación."))
        .andExpect(jsonPath("$.puedeEntregar").value(false));

    mvc.perform(entrega(tareaId, "ensayo-final.pdf"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("ya calificó")));
  }

  @Test
  void rectificarLaNotaExigeMotivoPeroCambiarSoloElComentarioNo() throws Exception {
    EvaluacionParcial evaluacion = evaluacion("Ensayo argumentativo", "20.00", 4);
    entityManager.flush();
    int tareaId = idDe(crearTarea("Ensayo", null, evaluacion.getId()));

    calificar(tareaId, "C", false, null, null).andExpect(status().isOk());
    calificar(tareaId, "B", false, null, null)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("motivo")));
    calificar(tareaId, "B", false, "Error de digitación en la rúbrica", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entregas[0].nota").value("B"));
    calificar(tareaId, "B", false, null, "Falta la conclusión.")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entregas[0].comentario").value("Falta la conclusión."));
    calificar(tareaId, "B", false, null, "Falta la conclusión.")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("No hay cambios")));
  }

  @Test
  void unaTareaSinEvaluacionNoSeCalificaYSeVeComoAntes() throws Exception {
    int tareaId = idDe(crearTarea("Rutina de lectura", "10.00", null));

    calificar(tareaId, "A", false, null, null)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("no cuenta para la nota")));
    mvc.perform(get(RUTA_ALUMNO + "/{id}", tareaId).with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cuentaParaNota").value(false))
        .andExpect(jsonPath("$.nota").value(nullValue()))
        .andExpect(jsonPath("$.ponderacion").value(10.0));
  }

  @Test
  void alDesactivarLaEvaluacionLaTareaQuedaSinNota() throws Exception {
    EvaluacionParcial evaluacion = evaluacion("Ensayo argumentativo", "20.00", 4);
    entityManager.flush();
    int tareaId = idDe(crearTarea("Ensayo", null, evaluacion.getId()));

    mvc.perform(
            delete("/api/v1/evaluaciones-parciales/{id}", evaluacion.getId())
                .with(user(administrador).roles("ADMINISTRADOR")))
        .andExpect(status().is2xxSuccessful());

    mvc.perform(
            get("/api/v1/tareas")
                .param("periodoId", periodo.getId().toString())
                .with(user(docente).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(tareaId))
        .andExpect(jsonPath("$[0].evaluacionParcialId").value(nullValue()));
    calificar(tareaId, "A", false, null, null).andExpect(status().isUnprocessableEntity());
  }

  private EvaluacionParcial evaluacion(String titulo, String ponderacion, Integer bimestre) {
    EvaluacionParcial evaluacion = new EvaluacionParcial();
    evaluacion.setPeriodo(periodo);
    evaluacion.setCompetencia(competencia);
    evaluacion.setTitulo(titulo);
    evaluacion.setPonderacion(new BigDecimal(ponderacion));
    evaluacion.setBimestre(bimestre);
    entityManager.persist(evaluacion);
    return evaluacion;
  }

  private ResultActions crearTarea(String titulo, String ponderacion, Integer evaluacionId)
      throws Exception {
    String cuerpo =
        objectMapper
            .createObjectNode()
            .put("periodoId", periodo.getId())
            .put("competenciaCodigo", competencia.getCodigo())
            .put("titulo", titulo)
            .put("descripcion", "Consigna de " + titulo)
            .put("fechaLimite", ahora.plusDays(3).withNano(0).toString())
            .put("ponderacion", ponderacion == null ? null : new BigDecimal(ponderacion))
            .put("evaluacionParcialId", evaluacionId)
            .toString();
    return mvc.perform(
        post("/api/v1/tareas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo)
            .with(user(docente).roles("DOCENTE")));
  }

  private int idDe(ResultActions resultado) throws Exception {
    String cuerpo =
        resultado.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(cuerpo).get("id").asInt();
  }

  private ResultActions calificar(
      int tareaId, String nota, boolean justificada, String motivo, String comentario)
      throws Exception {
    String cuerpo =
        objectMapper
            .createObjectNode()
            .put("nota", nota)
            .put("justificada", justificada)
            .put("motivo", motivo)
            .put("comentario", comentario)
            .toString();
    return mvc.perform(
        put("/api/v1/tareas/{tareaId}/entregas/{matriculaId}/calificacion", tareaId, matriculaId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo)
            .with(user(docente).roles("DOCENTE")));
  }

  private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
      entrega(int tareaId, String nombre) {
    var solicitud =
        multipart(RUTA_ALUMNO + "/{id}/entrega", tareaId)
            .file(
                new MockMultipartFile(
                    "archivo",
                    nombre,
                    "application/pdf",
                    "contenido".getBytes(StandardCharsets.UTF_8)));
    solicitud.with(user(alumno).roles("ALUMNO"));
    return solicitud;
  }
}
