package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.*;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin-libretas-test", roles = "ADMINISTRADOR")
@TestPropertySource(properties = "app.storage.evidence-dir=target/test-evidencias")
@Transactional
class LibretaNotasIntegrationTest {

  @Autowired MockMvc mvc;
  @PersistenceContext EntityManager entityManager;

  private Integer estudianteId;
  private Integer periodoSolicitadoId;
  private Integer periodoCerradoId;
  private Integer periodoAbiertoAnteriorId;
  private String competenciaCodigo;
  private String apoderadoUsername;
  private String apoderadoNoVinculadoUsername;
  private String docenteUsername;
  private String alumnoUsername;
  private Integer seccionId;

  @BeforeEach
  void prepararDatos() {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    Persona personaEstudiante = new Persona("T" + sufijo, "Ana", "Pérez", "López");
    entityManager.persist(personaEstudiante);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(personaEstudiante);
    entityManager.persist(estudiante);
    alumnoUsername = "alumno-" + sufijo;
    entityManager.persist(new CuentaUsuario(alumnoUsername, "hash", Rol.ALUMNO, personaEstudiante));

    Grado grado = new Grado();
    grado.setNumero(Math.abs(sufijo.hashCode()) + 1);
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccion = new Seccion();
    seccion.setCodigo("A-" + sufijo);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    entityManager.flush();
    seccionId = seccion.getId();

    Periodo cerrado =
        nuevoPeriodo(grado, seccion, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 30), true);
    Periodo abiertoAnterior =
        nuevoPeriodo(grado, seccion, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 30), false);
    Periodo solicitado =
        nuevoPeriodo(grado, seccion, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20), false);
    entityManager.flush();
    periodoCerradoId = cerrado.getId();
    periodoAbiertoAnteriorId = abiertoAnterior.getId();
    periodoSolicitadoId = solicitado.getId();

    Matricula matriculaCerrada = matricular(estudiante, cerrado);
    matricular(estudiante, abiertoAnterior);
    Matricula matriculaSolicitada = matricular(estudiante, solicitado);
    entityManager.flush();

    competenciaCodigo = "MAT-" + sufijo;
    Competencia competencia = new Competencia();
    competencia.setCodigo(competenciaCodigo);
    competencia.setNombre("Resuelve problemas de cantidad");
    entityManager.persist(competencia);
    Area area = new Area();
    area.setCodigo("MAT-" + sufijo);
    area.setNombre("Matemática");
    entityManager.persist(area);
    entityManager.flush();
    area.getCompetencias().add(competencia);
    competencia.getAreas().add(area);
    entityManager.flush();

    guardarNota(cerrado, matriculaCerrada, competencia, "B", "Avanza en resolución");
    guardarNota(solicitado, matriculaSolicitada, competencia, "AD", "Logro destacado");

    Persona personaAdmin = new Persona("A" + sufijo, "Admin", "Prueba", "Local");
    entityManager.persist(personaAdmin);
    entityManager.persist(
        new CuentaUsuario("admin-libretas-test", "hash", Rol.ADMINISTRADOR, personaAdmin));

    Persona personaApoderado = new Persona("P" + sufijo, "Patricia", "Pérez", "López");
    entityManager.persist(personaApoderado);
    Apoderado apoderado = new Apoderado();
    apoderado.setPersona(personaApoderado);
    entityManager.persist(apoderado);
    entityManager.persist(
        new CuentaUsuario("apoderado-" + sufijo, "hash", Rol.APODERADO, personaApoderado));
    apoderadoUsername = "apoderado-" + sufijo;
    Persona personaApoderadoNoVinculado = new Persona("N" + sufijo, "No", "Vinculada", "Prueba");
    entityManager.persist(personaApoderadoNoVinculado);
    apoderadoNoVinculadoUsername = "apoderado-no-vinculado-" + sufijo;
    entityManager.persist(
        new CuentaUsuario(
            apoderadoNoVinculadoUsername, "hash", Rol.APODERADO, personaApoderadoNoVinculado));

    VinculoFamiliar vinculo = new VinculoFamiliar();
    vinculo.setEstudiante(estudiante);
    vinculo.setApoderado(apoderado);
    vinculo.setParentesco(Parentesco.MADRE);
    vinculo.setEsPrincipal(true);
    entityManager.persist(vinculo);

    Persona personaDocente = new Persona("D" + sufijo, "Daniel", "Docente", "Prueba");
    entityManager.persist(personaDocente);
    entityManager.persist(
        new CuentaUsuario("docente-" + sufijo, "hash", Rol.DOCENTE, personaDocente));
    docenteUsername = "docente-" + sufijo;
    Docente docente = new Docente();
    docente.setPersona(personaDocente);
    entityManager.persist(docente);
    SesionClase sesion = new SesionClase();
    sesion.setSeccion(seccion);
    sesion.setArea(area);
    sesion.setDocente(docente);
    sesion.setFecha(LocalDate.of(2026, 9, 1));
    sesion.setHoraInicio(LocalTime.of(9, 0));
    sesion.setHoraFin(LocalTime.of(10, 0));
    entityManager.persist(sesion);
    entityManager.flush();

    estudianteId = estudiante.getId();
  }

  @Test
  void generaLibretaAgrupadaYAcumulaSoloPeriodosCerradosAnterioresDelMismoAnhio() throws Exception {
    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", estudianteId)
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estudiante").value("Ana Pérez López"))
        .andExpect(jsonPath("$.anhioEscolar").value(2026))
        .andExpect(jsonPath("$.periodos.length()").value(2))
        .andExpect(jsonPath("$.periodos[0].periodoId").value(periodoCerradoId))
        .andExpect(jsonPath("$.periodos[0].actasCerradas").value(true))
        .andExpect(jsonPath("$.periodos[0].areas[0].nombre").value("Matemática"))
        .andExpect(jsonPath("$.periodos[0].areas[0].competencias[0].nota").value("B"))
        .andExpect(jsonPath("$.periodos[1].periodoId").value(periodoSolicitadoId))
        .andExpect(jsonPath("$.periodos[1].areas[0].competencias[0].nota").value("AD"));
  }

  @Test
  void apoderadoPuedeVerSoloEstudianteVinculado() throws Exception {
    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", estudianteId)
                .with(user(apoderadoUsername).roles("APODERADO"))
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodos.length()").value(2));

    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", estudianteId)
                .with(user(apoderadoNoVinculadoUsername).roles("APODERADO"))
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void docenteVeCompetenciasDeSuClaseYNoPuedeConsultarOtraSeccion() throws Exception {
    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", estudianteId)
                .with(user(docenteUsername).roles("DOCENTE"))
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.periodos[1].areas[0].competencias[0].codigo").value(competenciaCodigo));

    Integer otroPeriodo = crearPeriodoEnOtraSeccion();
    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", estudianteId)
                .with(user(docenteUsername).roles("DOCENTE"))
                .param("periodoId", otroPeriodo.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void alumnoConsultaSoloSuPropiaLibreta() throws Exception {
    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", estudianteId)
                .with(user(alumnoUsername).roles("ALUMNO"))
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isOk());

    Integer otroEstudianteId = crearEstudianteSinMatricula();
    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", otroEstudianteId)
                .with(user(alumnoUsername).roles("ALUMNO"))
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void pdfDePeriodoCerradoEsOficialYElDePeriodoAbiertoEsBorradorConMarcaYFecha() throws Exception {
    MvcResult oficial =
        mvc.perform(
                get("/api/v1/estudiantes/{estudianteId}/libreta/pdf", estudianteId)
                    .param("periodoId", periodoCerradoId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Documento-Estado", "OFICIAL"))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("-oficial.pdf")))
            .andReturn();
    String textoOficial = textoPdf(oficial);
    assertTrue(textoOficial.contains("LIBRETA OFICIAL DE NOTAS"));
    assertFalse(textoOficial.contains("DOCUMENTO NO OFICIAL"));

    MvcResult preliminar =
        mvc.perform(
                get("/api/v1/estudiantes/{estudianteId}/libreta/pdf", estudianteId)
                    .param("periodoId", periodoSolicitadoId.toString()))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Documento-Estado", "BORRADOR"))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("-borrador.pdf")))
            .andReturn();
    String textoPreliminar = textoPdf(preliminar);
    assertTrue(textoPreliminar.contains("INFORME DE PROGRESO - BORRADOR"));
    assertTrue(textoPreliminar.contains("BORRADOR - DOCUMENTO NO OFICIAL"));
    assertTrue(textoPreliminar.contains("Consulta realizada:"));
    assertTrue(
        new String(preliminar.getResponse().getContentAsByteArray(), StandardCharsets.ISO_8859_1)
            .startsWith("%PDF-"));
  }

  @Test
  void alumnoNoPuedeDescargarPdf() throws Exception {
    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta/pdf", estudianteId)
                .with(user(alumnoUsername).roles("ALUMNO"))
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void estudianteSinMatriculaConfirmadaEnPeriodoRecibeNotFound() throws Exception {
    String dni = "S" + UUID.randomUUID().toString().substring(0, 8);
    Persona persona = new Persona(dni, "Sin", "Matricula", "Confirmada");
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);
    entityManager.flush();

    mvc.perform(
            get("/api/v1/estudiantes/{estudianteId}/libreta", estudiante.getId())
                .param("periodoId", periodoSolicitadoId.toString()))
        .andExpect(status().isNotFound());
  }

  private Periodo nuevoPeriodo(
      Grado grado, Seccion seccion, LocalDate desde, LocalDate hasta, boolean actasCerradas) {
    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(desde);
    periodo.setFechaFin(hasta);
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    periodo.setActasCerradas(actasCerradas);
    if (actasCerradas) {
      periodo.setFechaCierreActas(LocalDateTime.of(2026, 6, 1, 10, 0));
      periodo.setUsuarioCierreActas("admin");
    }
    entityManager.persist(periodo);
    return periodo;
  }

  private Matricula matricular(Estudiante estudiante, Periodo periodo) {
    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(periodo.getFechaInicio());
    matricula.setEstado(EstadoMatricula.CONFIRMADA);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);
    return matricula;
  }

  private void guardarNota(
      Periodo periodo,
      Matricula matricula,
      Competencia competencia,
      String nota,
      String observacion) {
    Calificacion calificacion = new Calificacion();
    calificacion.setPeriodo(periodo);
    calificacion.setMatricula(matricula);
    calificacion.setCompetencia(competencia);
    calificacion.setNota(nota);
    calificacion.setObservacion(observacion);
    calificacion.setFechaRegistro(LocalDateTime.now());
    entityManager.persist(calificacion);
  }

  private Integer crearPeriodoEnOtraSeccion() {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    Seccion seccionOriginal = entityManager.find(Seccion.class, seccionId);
    Seccion otraSeccion = new Seccion();
    otraSeccion.setCodigo("B-" + sufijo);
    otraSeccion.setVacantesTotales(30);
    seccionOriginal.getGrado().agregarSeccion(otraSeccion);
    entityManager.persist(otraSeccion);
    Periodo otro = new Periodo();
    otro.setGrado(otraSeccion.getGrado());
    otro.setSeccion(otraSeccion);
    otro.setFechaInicio(LocalDate.of(2026, 8, 2));
    otro.setFechaFin(LocalDate.of(2026, 12, 20));
    otro.setEstado(EstadoPeriodo.EN_CURSO);
    entityManager.persist(otro);
    entityManager.flush();
    return otro.getId();
  }

  private Integer crearEstudianteSinMatricula() {
    String dni = "S" + UUID.randomUUID().toString().substring(0, 8);
    Persona persona = new Persona(dni, "Sin", "Matricula", "Confirmada");
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);
    entityManager.flush();
    return estudiante.getId();
  }

  private String textoPdf(MvcResult resultado) throws Exception {
    PdfReader lector =
        new PdfReader(new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()));
    try {
      return new PdfTextExtractor(lector).getTextFromPage(1, false);
    } finally {
      lector.close();
    }
  }
}
