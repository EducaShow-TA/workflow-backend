package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.model.Competencia;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin-cuadro-merito-test", roles = "ADMINISTRADOR")
@TestPropertySource(properties = "app.storage.evidence-dir=target/test-evidencias")
@Transactional
class CuadroMeritoIntegrationTest {

  @Autowired MockMvc mvc;
  @PersistenceContext EntityManager entityManager;

  private Integer periodoCerradoId;
  private Integer periodoAbiertoId;
  private Integer estudianteAnaId;
  private Integer estudianteBetoId;
  private Integer estudianteCaroId;

  @BeforeEach
  void prepararDatos() {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    Grado grado = new Grado();
    grado.setNumero(Math.floorMod(sufijo.hashCode(), 100_000) + 1);
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccion = new Seccion();
    seccion.setCodigo("A-" + sufijo);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    entityManager.flush();

    Periodo cerrado =
        nuevoPeriodo(grado, seccion, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20), true);
    Periodo abierto =
        nuevoPeriodo(grado, seccion, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 12, 20), false);
    entityManager.flush();
    periodoCerradoId = cerrado.getId();
    periodoAbiertoId = abierto.getId();

    Estudiante ana = nuevoEstudiante("Ana", "A");
    Estudiante beto = nuevoEstudiante("Beto", "B");
    Estudiante caro = nuevoEstudiante("Caro", "C");
    Estudiante dora = nuevoEstudiante("Dora", "D");
    Matricula matriculaAna = matricular(ana, cerrado);
    Matricula matriculaBeto = matricular(beto, cerrado);
    Matricula matriculaCaro = matricular(caro, cerrado);
    matricular(dora, cerrado);

    Competencia competenciaUno = nuevaCompetencia("COMP-" + sufijo + "-1");
    Competencia competenciaDos = nuevaCompetencia("COMP-" + sufijo + "-2");
    guardarCalificacion(cerrado, matriculaAna, competenciaUno, "AD", "4.0000");
    guardarCalificacion(cerrado, matriculaAna, competenciaDos, "B", "2.0000");
    guardarCalificacion(cerrado, matriculaBeto, competenciaUno, "A", "3.0000");
    guardarCalificacion(cerrado, matriculaCaro, competenciaUno, "B", null);
    entityManager.flush();

    estudianteAnaId = ana.getId();
    estudianteBetoId = beto.getId();
    estudianteCaroId = caro.getId();
  }

  @Test
  void calculaPuestosCompartidosIncluyeTodaLaMatriculaYExportaPdfYExcel() throws Exception {
    mvc.perform(
            get("/api/v1/reportes/cuadro-merito").param("periodoId", periodoCerradoId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anhioEscolar").value(2026))
        .andExpect(jsonPath("$.estudiantes.length()").value(4))
        .andExpect(jsonPath("$.estudiantes[0].estudianteId").value(estudianteAnaId))
        .andExpect(jsonPath("$.estudiantes[0].puesto").value(1))
        .andExpect(jsonPath("$.estudiantes[0].promedio").value(3.0))
        .andExpect(jsonPath("$.estudiantes[1].estudianteId").value(estudianteBetoId))
        .andExpect(jsonPath("$.estudiantes[1].puesto").value(1))
        .andExpect(jsonPath("$.estudiantes[2].estudianteId").value(estudianteCaroId))
        .andExpect(jsonPath("$.estudiantes[2].puesto").value(3))
        .andExpect(jsonPath("$.estudiantes[2].promedio").value(2.0))
        .andExpect(jsonPath("$.estudiantes[3].puesto").doesNotExist());

    MvcResult pdf =
        mvc.perform(
                get("/api/v1/reportes/cuadro-merito/pdf")
                    .param("periodoId", periodoCerradoId.toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(
                header()
                    .string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
            .andReturn();
    PdfReader lector =
        new PdfReader(new ByteArrayInputStream(pdf.getResponse().getContentAsByteArray()));
    try {
      assertTrue(new PdfTextExtractor(lector).getTextFromPage(1).contains("CUADRO DE MÉRITO"));
    } finally {
      lector.close();
    }

    MvcResult excel =
        mvc.perform(
                get("/api/v1/reportes/cuadro-merito/excel")
                    .param("periodoId", periodoCerradoId.toString()))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(
                header()
                    .string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
            .andReturn();
    try (var libro =
        WorkbookFactory.create(
            new ByteArrayInputStream(excel.getResponse().getContentAsByteArray()))) {
      assertEquals(
          "CUADRO DE MÉRITO", libro.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
      assertEquals("Ana A Test", libro.getSheetAt(0).getRow(5).getCell(1).getStringCellValue());
    }
  }

  @Test
  void noGeneraRankingSinCierreYRestringeAccesoAlAdministrador() throws Exception {
    mvc.perform(
            get("/api/v1/reportes/cuadro-merito").param("periodoId", periodoAbiertoId.toString()))
        .andExpect(status().isConflict());
    mvc.perform(
            get("/api/v1/reportes/cuadro-merito")
                .with(user("docente").roles("DOCENTE"))
                .param("periodoId", periodoCerradoId.toString()))
        .andExpect(status().isForbidden());
  }

  private Periodo nuevoPeriodo(
      Grado grado, Seccion seccion, LocalDate desde, LocalDate hasta, boolean cerrado) {
    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(desde);
    periodo.setFechaFin(hasta);
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    periodo.setActasCerradas(cerrado);
    if (cerrado) periodo.setFechaCierreActas(LocalDateTime.of(2026, 12, 21, 10, 0));
    entityManager.persist(periodo);
    return periodo;
  }

  private Estudiante nuevoEstudiante(String nombre, String dniPrefijo) {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    Persona persona = new Persona(dniPrefijo + sufijo, nombre, "A", "Test");
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);
    return estudiante;
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

  private Competencia nuevaCompetencia(String codigo) {
    Competencia competencia = new Competencia();
    competencia.setCodigo(codigo);
    competencia.setNombre(codigo);
    entityManager.persist(competencia);
    return competencia;
  }

  private void guardarCalificacion(
      Periodo periodo, Matricula matricula, Competencia competencia, String nota, String puntaje) {
    Calificacion calificacion = new Calificacion();
    calificacion.setPeriodo(periodo);
    calificacion.setMatricula(matricula);
    calificacion.setCompetencia(competencia);
    calificacion.setNota(nota);
    calificacion.setPuntajeConsolidado(puntaje == null ? null : new java.math.BigDecimal(puntaje));
    calificacion.setConsolidada(puntaje != null);
    calificacion.setFechaRegistro(LocalDateTime.now());
    entityManager.persist(calificacion);
  }
}
