package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.fixture.HorarioFixture;
import pe.edu.pucp.colegio.common.model.*;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.storage.evidence-dir=target/test-evidencias")
class TareasEstudianteIntegrationTest {

  private static final String RUTA = "/api/v1/estudiantes/mis-tareas";

  @Autowired MockMvc mvc;
  @Autowired Clock clock;
  @PersistenceContext EntityManager entityManager;

  private String sufijo;
  private String alumno;
  private String docente;
  private Integer matriculaId;
  private Periodo periodo;
  private Competencia competencia;
  private Area area;
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
    seccion.setCodigo("T-" + sufijo);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    periodo = periodo(grado, seccion, anio);

    competencia = new Competencia();
    competencia.setCodigo("COM-" + sufijo);
    competencia.setNombre("Escribe diversos tipos de textos " + sufijo);
    entityManager.persist(competencia);
    area = new Area();
    area.setCodigo("AREA-" + sufijo);
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

    Persona personaAlumno = new Persona("E" + sufijo, "Lucía", "Ramos", "Díaz");
    entityManager.persist(personaAlumno);
    alumno = "alumno-" + sufijo;
    entityManager.persist(new CuentaUsuario(alumno, "hash", Rol.ALUMNO, personaAlumno));
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(personaAlumno);
    entityManager.persist(estudiante);
    matriculaId = matricular(estudiante, periodo).getId();
    entityManager.flush();
  }

  @Test
  void listaSoloLasTareasDeSuSeccionConEstadoAreaYDocente() throws Exception {
    Tarea vencida = tarea(periodo, "Collage", ahora.minusDays(1));
    Tarea pendiente = tarea(periodo, "Ensayo", ahora.plusDays(2));
    Tarea entregada = tarea(periodo, "Ficha", ahora.plusDays(5));
    marcarCumplida(entregada);
    Grado otroGrado = new Grado();
    otroGrado.setNumero(Math.abs(sufijo.hashCode()) + 2);
    otroGrado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion otraSeccion = new Seccion();
    otraSeccion.setCodigo("U-" + sufijo);
    otraSeccion.setVacantesTotales(30);
    otroGrado.agregarSeccion(otraSeccion);
    entityManager.persist(otroGrado);
    tarea(periodo(otroGrado, otraSeccion, ahora.getYear()), "Ajena", ahora.plusDays(1));
    entityManager.flush();

    mvc.perform(get(RUTA).with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].id").value(vencida.getId()))
        .andExpect(jsonPath("$[0].estado").value("VENCIDA"))
        .andExpect(jsonPath("$[0].puedeEntregar").value(false))
        .andExpect(jsonPath("$[1].id").value(pendiente.getId()))
        .andExpect(jsonPath("$[1].estado").value("PENDIENTE"))
        .andExpect(jsonPath("$[1].puedeEntregar").value(true))
        .andExpect(jsonPath("$[1].area").value(area.getNombre()))
        .andExpect(jsonPath("$[1].docente").value(containsString("Rojas")))
        .andExpect(jsonPath("$[1].descripcion").value("Consigna de Ensayo"))
        .andExpect(jsonPath("$[2].id").value(entregada.getId()))
        .andExpect(jsonPath("$[2].estado").value("ENTREGADA"))
        .andExpect(jsonPath("$[2].origen").value("DOCENTE"));
  }

  @Test
  void entregaUnArchivoLoReemplazaAntesDelPlazoYElDocentePuedeDescargarlo() throws Exception {
    Tarea tarea = tarea(periodo, "Ensayo", ahora.plusDays(2));
    entityManager.flush();

    mvc.perform(entrega(tarea.getId(), "ensayo.pdf", "application/pdf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ENTREGADA"))
        .andExpect(jsonPath("$.origen").value("ALUMNO"))
        .andExpect(jsonPath("$.archivoNombre").value("ensayo.pdf"))
        .andExpect(jsonPath("$.fechaEntrega").isNotEmpty());
    mvc.perform(entrega(tarea.getId(), "ensayo-final.docx", "application/octet-stream"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.archivoNombre").value("ensayo-final.docx"));

    mvc.perform(
            get(RUTA + "/{id}/entrega/archivo", tarea.getId()).with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("ensayo-final.docx")))
        .andExpect(content().bytes("contenido".getBytes(StandardCharsets.UTF_8)));
    mvc.perform(
            get("/api/v1/tareas/{id}/entregas/{matricula}/archivo", tarea.getId(), matriculaId)
                .with(user(docente).roles("DOCENTE")))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/tareas")
                .param("periodoId", periodo.getId().toString())
                .with(user(docente).roles("DOCENTE")))
        .andExpect(jsonPath("$[0].entregas[0].origen").value("ALUMNO"))
        .andExpect(jsonPath("$[0].entregas[0].archivoNombre").value("ensayo-final.docx"))
        .andExpect(jsonPath("$[0].entregas[0].archivoTamanio").value(9));
  }

  @Test
  void rechazaTareaVencidaTipoNoPermitidoYTareaDeOtraSeccion() throws Exception {
    Tarea vencida = tarea(periodo, "Collage", ahora.minusMinutes(1));
    Tarea pendiente = tarea(periodo, "Ensayo", ahora.plusDays(2));
    Grado otroGrado = new Grado();
    otroGrado.setNumero(Math.abs(sufijo.hashCode()) + 3);
    otroGrado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion otraSeccion = new Seccion();
    otraSeccion.setCodigo("V-" + sufijo);
    otraSeccion.setVacantesTotales(30);
    otroGrado.agregarSeccion(otraSeccion);
    entityManager.persist(otroGrado);
    Tarea ajena =
        tarea(periodo(otroGrado, otraSeccion, ahora.getYear()), "Ajena", ahora.plusDays(1));
    entityManager.flush();

    mvc.perform(entrega(vencida.getId(), "collage.pdf", "application/pdf"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("fecha límite")));
    mvc.perform(entrega(pendiente.getId(), "tarea.exe", "application/octet-stream"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value(containsString("Tipo de archivo no permitido")));
    mvc.perform(entrega(ajena.getId(), "tarea.pdf", "application/pdf"))
        .andExpect(status().isForbidden());
    mvc.perform(get(RUTA + "/{id}", ajena.getId()).with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isForbidden());
    mvc.perform(
            get(RUTA + "/{id}/entrega/archivo", pendiente.getId())
                .with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isNotFound());
    mvc.perform(get(RUTA + "/{id}", 999_999).with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isNotFound());
  }

  @Test
  void siElDocenteRevierteLaEntregaVuelveAPendienteYConservaElArchivo() throws Exception {
    Tarea tarea = tarea(periodo, "Ensayo", ahora.plusDays(2));
    entityManager.flush();
    mvc.perform(entrega(tarea.getId(), "ensayo.pdf", "application/pdf")).andExpect(status().isOk());

    mvc.perform(
            patch("/api/v1/tareas/{id}/entregas/{matricula}", tarea.getId(), matriculaId)
                .with(user(docente).roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cumplida\":false}"))
        .andExpect(status().isOk());

    mvc.perform(get(RUTA + "/{id}", tarea.getId()).with(user(alumno).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
        .andExpect(jsonPath("$.fechaEntrega").doesNotExist())
        .andExpect(jsonPath("$.archivoNombre").value("ensayo.pdf"));
  }

  @Test
  void otrosRolesNoPuedenUsarElPortalDeTareasDelAlumno() throws Exception {
    mvc.perform(get(RUTA).with(user(docente).roles("DOCENTE"))).andExpect(status().isForbidden());
    mvc.perform(get(RUTA).with(user("admin-" + sufijo).roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
  }

  private MockMultipartHttpServletRequestBuilder entrega(
      Integer tareaId, String nombre, String tipo) {
    MockMultipartHttpServletRequestBuilder solicitud =
        multipart(RUTA + "/{id}/entrega", tareaId)
            .file(
                new MockMultipartFile(
                    "archivo", nombre, tipo, "contenido".getBytes(StandardCharsets.UTF_8)));
    solicitud.with(user(alumno).roles("ALUMNO"));
    return solicitud;
  }

  private Periodo periodo(Grado grado, Seccion seccion, int anio) {
    Periodo nuevo = new Periodo();
    nuevo.setGrado(grado);
    nuevo.setSeccion(seccion);
    nuevo.setFechaInicio(LocalDate.of(anio, 1, 1));
    nuevo.setFechaFin(LocalDate.of(anio, 12, 31));
    nuevo.setEstado(EstadoPeriodo.EN_CURSO);
    entityManager.persist(nuevo);
    return nuevo;
  }

  private Matricula matricular(Estudiante estudiante, Periodo destino) {
    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(destino.getFechaInicio());
    matricula.setEstado(EstadoMatricula.CONFIRMADA);
    matricula.setPeriodo(destino);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);
    return matricula;
  }

  private Tarea tarea(Periodo destino, String titulo, LocalDateTime fechaLimite) {
    Tarea tarea = new Tarea();
    tarea.setPeriodo(destino);
    tarea.setCompetencia(competencia);
    tarea.setTitulo(titulo);
    tarea.setDescripcion("Consigna de " + titulo);
    tarea.setFechaLimite(fechaLimite);
    tarea.setPonderacion(new BigDecimal("20.00"));
    entityManager.persist(tarea);
    return tarea;
  }

  private void marcarCumplida(Tarea tarea) {
    EntregaTarea entrega = new EntregaTarea();
    entrega.setTarea(tarea);
    entrega.setMatricula(entityManager.find(Matricula.class, matriculaId));
    entrega.setCumplida(true);
    entrega.setFechaCumplimiento(ahora.minusHours(1));
    entrega.setOrigen(OrigenEntregaTarea.DOCENTE);
    entityManager.persist(entrega);
  }
}
