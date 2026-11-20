package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.fixture.HorarioFixture;
import pe.edu.pucp.colegio.common.model.*;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = SeguimientoAcademicoIntegrationTest.USUARIO_DOCENTE, roles = "DOCENTE")
@TestPropertySource(properties = "app.storage.evidence-dir=target/test-evidencias")
@Transactional
class SeguimientoAcademicoIntegrationTest {

  static final String USUARIO_DOCENTE = "docente-seguimiento-test";

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

    Area area = new Area();
    area.setCodigo("AREA-" + sufijo);
    area.setNombre("Matemática " + sufijo);
    area.getCompetencias().add(competencia);
    entityManager.persist(area);

    Persona personaDocente = new Persona("D" + sufijo, "Docente", "Prueba", sufijo);
    entityManager.persist(personaDocente);
    entityManager.persist(
        new CuentaUsuario(USUARIO_DOCENTE, "hash-de-prueba", Rol.DOCENTE, personaDocente));
    Docente docente = new Docente();
    docente.setPersona(personaDocente);
    entityManager.persist(docente);
    HorarioFixture.asignar(
        entityManager,
        seccion,
        area,
        USUARIO_DOCENTE,
        // El horario solo admite lunes a viernes (chk_sesion_dia): con LocalDate.now() la prueba
        // fallaba los fines de semana.
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        java.time.LocalTime.of(8, 0),
        java.time.LocalTime.of(8, 45));

    SesionClase sesion = new SesionClase();
    sesion.setSeccion(seccion);
    sesion.setArea(area);
    sesion.setDocente(docente);
    sesion.setFecha(LocalDate.now());
    sesion.setHoraInicio(java.time.LocalTime.of(8, 0));
    sesion.setHoraFin(java.time.LocalTime.of(8, 45));
    entityManager.persist(sesion);

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
    String solicitud =
        """
        {"periodoId":%d,"competenciaCodigo":"%s","titulo":"Práctica de fracciones",
        "descripcion":"Resolver los ejercicios asignados.","fechaLimite":"2026-11-20T23:59:00","ponderacion":20.00}
        """
            .formatted(periodoId, competenciaCodigo);

    MvcResult resultado =
        mvc.perform(
                post("/api/v1/tareas").contentType(MediaType.APPLICATION_JSON).content(solicitud))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("Práctica de fracciones"))
            .andExpect(jsonPath("$.ponderacion").value(20.0))
            .andExpect(jsonPath("$.entregas.length()").value(1))
            .andExpect(jsonPath("$.entregas[0].estudiante").value("Ana Pérez Pérez"))
            .andExpect(jsonPath("$.entregas[0].cumplida").value(false))
            .andReturn();
    int tareaId =
        objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asInt();

    mvc.perform(
            patch("/api/v1/tareas/{tareaId}/entregas/{matriculaId}", tareaId, matriculaId)
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
    CalificacionRequestBody datos =
        new CalificacionRequestBody(
            periodoId,
            matriculaId,
            competenciaCodigo,
            "AD",
            "Resolución completa",
            "https://example.org/evidencia");

    mvc.perform(
            multipart("/api/v1/calificaciones")
                .part(partJson("datos", objectMapper.writeValueAsBytes(datos))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nota").value("AD"))
        .andExpect(jsonPath("$.evidenciaTipo").value("URL"))
        .andExpect(jsonPath("$.evidenciaUrl").value("https://example.org/evidencia"));

    mvc.perform(
            get("/api/v1/seguimiento/competencias/{codigo}", competenciaCodigo)
                .param("periodoId", periodoId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].estudiante").value("Ana Pérez Pérez"))
        .andExpect(jsonPath("$[0].nota").value("AD"))
        .andExpect(jsonPath("$[0].evidenciaUrl").value("https://example.org/evidencia"));
  }

  @Test
  void registraAuditoriaDeCreacionYActualizacionDeCalificacionFinal() throws Exception {
    mvc.perform(
            multipart("/api/v1/calificaciones")
                .part(
                    partJson(
                        "datos",
                        objectMapper.writeValueAsBytes(
                            new CalificacionRequestBody(
                                periodoId,
                                matriculaId,
                                competenciaCodigo,
                                "AD",
                                "Primera observación",
                                "https://example.org/evidencia")))))
        .andExpect(status().isOk());

    mvc.perform(
            multipart("/api/v1/calificaciones")
                .part(
                    partJson(
                        "datos",
                        objectMapper.writeValueAsBytes(
                            new CalificacionRequestBody(
                                periodoId,
                                matriculaId,
                                competenciaCodigo,
                                "A",
                                "Observación actualizada",
                                "https://example.org/evidencia",
                                "Error de digitación al pasar las notas")))))
        .andExpect(status().isOk());

    mvc.perform(
            get("/api/v1/logs/calificaciones")
                .with(user("admin-auditoria-test").roles("ADMINISTRADOR"))
                .param("periodoId", periodoId.toString())
                .param("matriculaId", matriculaId.toString())
                .param("tipo", "FINAL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].usuario").value("docente-seguimiento-test"))
        .andExpect(jsonPath("$.content[0].rolUsuario").value("DOCENTE"))
        .andExpect(jsonPath("$.content[0].accion").value("RECTIFICAR"))
        .andExpect(jsonPath("$.content[0].motivo").value("Error de digitación al pasar las notas"))
        .andExpect(jsonPath("$.content[0].notaAnterior").value("AD"))
        .andExpect(jsonPath("$.content[0].notaNueva").value("A"))
        .andExpect(jsonPath("$.content[0].observacionAnterior").value("Primera observación"))
        .andExpect(jsonPath("$.content[0].observacionNueva").value("Observación actualizada"))
        .andExpect(jsonPath("$.content[1].accion").value("REGISTRAR_O_ACTUALIZAR"))
        .andExpect(jsonPath("$.content[1].motivo").doesNotExist())
        .andExpect(jsonPath("$.content[1].notaAnterior").doesNotExist())
        .andExpect(jsonPath("$.content[1].notaNueva").value("AD"));
  }

  @Test
  void rectificarExigeCambiosYMotivoYConservaLaEvidencia() throws Exception {
    MvcResult registro =
        enviar(nota("AD", "Buen trabajo", "https://example.org/evidencia", null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rectificaciones").value(0))
            .andReturn();
    int calificacionId =
        objectMapper.readTree(registro.getResponse().getContentAsString()).get("id").asInt();

    enviar(nota("AD", "Buen trabajo", null, "Error de digitación al registrar"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("No hay cambios que guardar"));
    enviar(nota("A", "Buen trabajo", null, null))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("motivo de la rectificación")));
    enviar(nota("A", "Buen trabajo", null, "  corto  "))
        .andExpect(status().isUnprocessableEntity());

    enviar(nota("A", "Buen trabajo", null, "  Revisión de la evidencia del examen  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nota").value("A"))
        .andExpect(jsonPath("$.evidenciaTipo").value("URL"))
        .andExpect(jsonPath("$.evidenciaUrl").value("https://example.org/evidencia"))
        .andExpect(jsonPath("$.rectificaciones").value(1));

    mvc.perform(
            get("/api/v1/seguimiento/competencias/{codigo}", competenciaCodigo)
                .param("periodoId", periodoId.toString()))
        .andExpect(jsonPath("$[0].rectificaciones").value(1));

    mvc.perform(get("/api/v1/calificaciones/{id}/historial", calificacionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].accion").value("RECTIFICAR"))
        .andExpect(jsonPath("$[0].notaAnterior").value("AD"))
        .andExpect(jsonPath("$[0].notaNueva").value("A"))
        .andExpect(jsonPath("$[0].motivo").value("Revisión de la evidencia del examen"))
        .andExpect(jsonPath("$[0].usuario").value(USUARIO_DOCENTE))
        .andExpect(jsonPath("$[1].accion").value("REGISTRAR_O_ACTUALIZAR"))
        .andExpect(jsonPath("$[1].notaAnterior").doesNotExist())
        .andExpect(jsonPath("$[1].notaNueva").value("AD"));

    mvc.perform(
            get("/api/v1/logs/calificaciones")
                .with(user("admin-auditoria-test").roles("ADMINISTRADOR"))
                .param("matriculaId", matriculaId.toString())
                .param("accion", "RECTIFICACIONES"))
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].motivo").value("Revisión de la evidencia del examen"));
    mvc.perform(
            get("/api/v1/logs/calificaciones")
                .with(user("admin-auditoria-test").roles("ADMINISTRADOR"))
                .param("matriculaId", matriculaId.toString())
                .param("accion", "REGISTROS"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }

  @Test
  void rectificarSinNuevaEvidenciaConservaElArchivoAdjunto() throws Exception {
    MockMultipartFile archivo =
        new MockMultipartFile("archivo", "examen.txt", "text/plain", "respuestas".getBytes());
    mvc.perform(
            multipart("/api/v1/calificaciones")
                .file(archivo)
                .part(
                    partJson("datos", objectMapper.writeValueAsBytes(nota("B", null, null, null)))))
        .andExpect(status().isOk());

    enviar(nota("A", null, null, "Pregunta 4 mal corregida en el examen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nota").value("A"))
        .andExpect(jsonPath("$.evidenciaTipo").value("ARCHIVO"))
        .andExpect(jsonPath("$.evidenciaNombre").value("examen.txt"));
  }

  @Test
  void registrarSinEvidenciaEsValidoPorqueLaEvidenciaEsOpcional() throws Exception {
    enviar(nota("A", null, null, null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nota").value("A"))
        .andExpect(jsonPath("$.evidenciaTipo").value(nullValue()))
        .andExpect(jsonPath("$.evidenciaUrl").value(nullValue()));
  }

  @Test
  void otroDocenteNoPuedeVerElHistorialDeUnaNotaAjena() throws Exception {
    MvcResult registro = enviar(nota("C", null, "https://example.org/evidencia", null)).andReturn();
    int calificacionId =
        objectMapper.readTree(registro.getResponse().getContentAsString()).get("id").asInt();
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    Persona persona = new Persona("O" + sufijo, "Otro", "Docente", sufijo);
    entityManager.persist(persona);
    entityManager.persist(
        new CuentaUsuario("otro-docente-" + sufijo, "hash-de-prueba", Rol.DOCENTE, persona));
    Docente otro = new Docente();
    otro.setPersona(persona);
    entityManager.persist(otro);
    entityManager.flush();

    mvc.perform(
            get("/api/v1/calificaciones/{id}/historial", calificacionId)
                .with(user("otro-docente-" + sufijo).roles("DOCENTE")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adjuntaArchivoYPermiteRecuperarloDesdeLaFichaDeSeguimiento() throws Exception {
    CalificacionRequestBody datos =
        new CalificacionRequestBody(
            periodoId, matriculaId, competenciaCodigo, "A", "Trabajo revisado", null);
    MockMultipartFile archivo =
        new MockMultipartFile(
            "archivo", "trabajo.txt", "text/plain", "contenido de evidencia".getBytes());

    MvcResult resultado =
        mvc.perform(
                multipart("/api/v1/calificaciones")
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
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .bytes("contenido de evidencia".getBytes()));
  }

  @Test
  void rechazaPonderacionFueraDelRangoPermitido() throws Exception {
    String solicitud =
        """
        {"periodoId":%d,"competenciaCodigo":"%s","titulo":"Práctica",
        "descripcion":"Descripción","fechaLimite":"2026-11-20T23:59:00","ponderacion":101}
        """
            .formatted(periodoId, competenciaCodigo);

    mvc.perform(post("/api/v1/tareas").contentType(MediaType.APPLICATION_JSON).content(solicitud))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rechazaUrlDeEvidenciaQueNoSeaHttpOHttps() throws Exception {
    CalificacionRequestBody datos =
        new CalificacionRequestBody(
            periodoId, matriculaId, competenciaCodigo, "B", null, "ftp://example.org/evidencia");

    mvc.perform(
            multipart("/api/v1/calificaciones")
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

  private CalificacionRequestBody nota(
      String nota, String observacion, String evidenciaUrl, String motivo) {
    return new CalificacionRequestBody(
        periodoId, matriculaId, competenciaCodigo, nota, observacion, evidenciaUrl, motivo);
  }

  private ResultActions enviar(CalificacionRequestBody datos) throws Exception {
    return mvc.perform(
        multipart("/api/v1/calificaciones")
            .part(partJson("datos", objectMapper.writeValueAsBytes(datos))));
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
      String evidenciaUrl,
      String motivo) {

    CalificacionRequestBody(
        Integer periodoId,
        Integer matriculaId,
        String competenciaCodigo,
        String nota,
        String observacion,
        String evidenciaUrl) {
      this(periodoId, matriculaId, competenciaCodigo, nota, observacion, evidenciaUrl, null);
    }
  }
}
