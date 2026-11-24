package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.CalificacionParcial;
import pe.edu.pucp.colegio.common.model.Competencia;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.EvaluacionParcial;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.service.BimestreAcademico;
import pe.edu.pucp.colegio.common.service.CalendarioBimestralService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CalendarioBimestresIntegrationTest {

  private static final int ANHIO = 2041;
  private static final LocalDate INICIO = LocalDate.of(ANHIO, 3, 3);
  private static final LocalDate FIN = LocalDate.of(ANHIO, 12, 19);

  @TestConfiguration
  static class Reloj {
    @Bean
    @Primary
    Clock relojBimestres() {
      return Clock.fixed(
          LocalDateTime.of(ANHIO, 5, 20, 10, 0).atZone(ZoneId.of("America/Lima")).toInstant(),
          ZoneId.of("America/Lima"));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired CalendarioBimestralService calendario;
  @PersistenceContext EntityManager entityManager;

  private Periodo periodo;
  private Competencia competencia;
  private EvaluacionParcial evaluacion;
  private Matricula conNota;
  private Matricula sinNota;

  @BeforeEach
  void prepararAnhio() {
    String sufijo = UUID.randomUUID().toString().substring(0, 8);
    Grado grado = new Grado();
    grado.setNumero(Math.abs(sufijo.hashCode()) + 1);
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccion = new Seccion();
    seccion.setCodigo("B-" + sufijo);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(INICIO);
    periodo.setFechaFin(FIN);
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    entityManager.persist(periodo);

    competencia = new Competencia();
    competencia.setCodigo("BIM-" + sufijo);
    competencia.setNombre("Resuelve problemas " + sufijo);
    entityManager.persist(competencia);
    Area area = new Area();
    area.setCodigo("MAT-" + sufijo);
    area.setNombre("Matemática " + sufijo);
    area.getCompetencias().add(competencia);
    entityManager.persist(area);

    conNota = matricula("A" + sufijo, "Lucía");
    sinNota = matricula("Z" + sufijo, "Jorge");
    evaluacion = new EvaluacionParcial();
    evaluacion.setPeriodo(periodo);
    evaluacion.setCompetencia(competencia);
    evaluacion.setTitulo("Práctica calificada 1");
    evaluacion.setPonderacion(new BigDecimal("100.00"));
    evaluacion.setBimestre(1);
    entityManager.persist(evaluacion);
    CalificacionParcial nota = new CalificacionParcial();
    nota.setEvaluacion(evaluacion);
    nota.setMatricula(conNota);
    nota.setNota("A");
    nota.setJustificada(false);
    nota.setFechaRegistro(LocalDateTime.of(ANHIO, 4, 10, 9, 0));
    entityManager.persist(nota);
    entityManager.flush();
  }

  @Test
  void sinCalendarioSeProponenLosCuatroTramosDeSiempre() throws Exception {
    mvc.perform(
            get("/api/v1/bimestres").param("anhio", "" + ANHIO).with(user("doc").roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[0].configurado").value(false))
        .andExpect(jsonPath("$[0].fechaInicio").value("2041-03-03"))
        .andExpect(jsonPath("$[0].estado").value("POR_CERRAR"))
        .andExpect(jsonPath("$[1].estado").value("EN_CURSO"))
        .andExpect(jsonPath("$[3].fechaFin").value("2041-12-19"));

    for (LocalDate dia = INICIO; !dia.isAfter(FIN); dia = dia.plusDays(1)) {
      assertEquals(BimestreAcademico.actual(periodo, dia), calendario.actual(periodo, dia));
    }
  }

  @Test
  void elCalendarioGuardadoDefineElBimestreDeCadaFecha() throws Exception {
    guardarCalendario("2041-05-16", "2041-05-19").andExpect(status().isOk());

    mvc.perform(get("/api/v1/bimestres").param("anhio", "" + ANHIO).with(admin()))
        .andExpect(jsonPath("$[0].configurado").value(true))
        .andExpect(jsonPath("$[0].fechaFin").value("2041-05-16"))
        .andExpect(jsonPath("$[1].fechaInicio").value("2041-05-19"));
    assertEquals(1, calendario.actual(periodo, LocalDate.of(ANHIO, 5, 16)));
    assertEquals(1, calendario.actual(periodo, LocalDate.of(ANHIO, 5, 18)));
    assertEquals(2, calendario.actual(periodo, LocalDate.of(ANHIO, 7, 30)));
    assertEquals(3, calendario.actual(periodo, LocalDate.of(ANHIO, 8, 10)));
  }

  @Test
  void noSeCambiaElInicioDeUnBimestreQueYaEmpezo() throws Exception {
    guardarCalendario("2041-05-15", "2041-05-18").andExpect(status().isOk());

    guardarCalendario("2041-05-15", "2041-05-19")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message")
                .value("El bimestre 2 ya empezó el 18/05/2041; su inicio no se puede cambiar"));
  }

  @Test
  void losBimestresNoSeCruzan() throws Exception {
    guardarCalendario("2041-05-20", "2041-05-18")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "El bimestre 2 debe empezar después del 20/05/2041, cuando termina el bimestre 1"));
  }

  @Test
  void cerrarUnBimestreCompletaLasNotasFaltantesConC() throws Exception {
    guardarCalendario("2041-05-15", "2041-05-18").andExpect(status().isOk());

    mvc.perform(post("/api/v1/bimestres/" + ANHIO + "/2/cierre").with(admin()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message")
                .value("El bimestre 2 termina el 24/07/2041; podrás cerrarlo desde el 25/07/2041"));

    mvc.perform(post("/api/v1/bimestres/" + ANHIO + "/1/cierre").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bimestre.estado").value("CERRADO"))
        .andExpect(jsonPath("$.bimestre.usuarioCierre").value("admin-bim"))
        .andExpect(jsonPath("$.notasCompletadas").value(1));
    entityManager.flush();

    assertEquals(
        "C",
        jdbc.queryForObject(
            "select nota from calificaciones_parciales where evaluacion_id=? and matricula_id=? and active=true",
            String.class,
            evaluacion.getId(),
            sinNota.getId()));
    assertEquals(
        "A",
        jdbc.queryForObject(
            "select nota from calificaciones_parciales where evaluacion_id=? and matricula_id=? and active=true",
            String.class,
            evaluacion.getId(),
            conNota.getId()));

    mvc.perform(post("/api/v1/bimestres/" + ANHIO + "/1/cierre").with(admin()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("El bimestre 1 ya está cerrado"));
  }

  @Test
  void unBimestreCerradoBloqueaAlDocenteYNoAdmiteEvaluacionesNuevas() throws Exception {
    guardarCalendario("2041-05-15", "2041-05-18").andExpect(status().isOk());
    mvc.perform(post("/api/v1/bimestres/" + ANHIO + "/1/cierre").with(admin()))
        .andExpect(status().isOk());

    BusinessException error =
        assertThrows(
            BusinessException.class, () -> calendario.exigirRegistroPermitido(periodo, 1, false));
    assertEquals(
        "El bimestre 1 está cerrado: solo el administrador puede rectificar sus notas",
        error.getMessage());
    assertDoesNotThrow(() -> calendario.exigirRegistroPermitido(periodo, 1, true));
    assertDoesNotThrow(() -> calendario.exigirRegistroPermitido(periodo, 2, false));

    mvc.perform(
            post("/api/v1/evaluaciones-parciales")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"periodoId\":"
                        + periodo.getId()
                        + ",\"competenciaCodigo\":\""
                        + competencia.getCodigo()
                        + "\",\"titulo\":\"Examen tardío\",\"ponderacion\":20,\"bimestre\":1}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "El bimestre 1 está cerrado; no se pueden crear ni cambiar sus evaluaciones"));
  }

  @Test
  void elCierreRespetaElOrdenDeLosBimestres() throws Exception {
    guardarCalendario("2041-05-15", "2041-05-18").andExpect(status().isOk());
    entityManager.flush();
    jdbc.update(
        "update bimestres_academicos set fecha_fin='2041-05-19' where anhio=? and numero=2", ANHIO);
    jdbc.update(
        "update bimestres_academicos set fecha_inicio='2041-05-18', fecha_fin='2041-05-18' where anhio=? and numero=1",
        ANHIO);
    entityManager.clear();

    mvc.perform(post("/api/v1/bimestres/" + ANHIO + "/2/cierre").with(admin()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.message").value("Primero cierra el bimestre 1"));
  }

  private ResultActions guardarCalendario(String finPrimero, String inicioSegundo)
      throws Exception {
    String cuerpo =
        "{\"anhio\":"
            + ANHIO
            + ",\"fechaInicio\":\"2041-03-03\",\"fechaFin\":\"2041-12-19\",\"bimestres\":["
            + "{\"numero\":1,\"fechaInicio\":\"2041-03-03\",\"fechaFin\":\""
            + finPrimero
            + "\"},"
            + "{\"numero\":2,\"fechaInicio\":\""
            + inicioSegundo
            + "\",\"fechaFin\":\"2041-07-24\"},"
            + "{\"numero\":3,\"fechaInicio\":\"2041-08-10\",\"fechaFin\":\"2041-10-09\"},"
            + "{\"numero\":4,\"fechaInicio\":\"2041-10-12\",\"fechaFin\":\"2041-12-19\"}]}";
    return mvc.perform(
        put("/api/v1/periodos/anhio")
            .with(admin())
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
  }

  private Matricula matricula(String documento, String nombre) {
    Persona persona = new Persona(documento, nombre, "Ramos", "Díaz");
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);
    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(INICIO);
    matricula.setEstado(EstadoMatricula.CONFIRMADA);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);
    return matricula;
  }

  private static RequestPostProcessor admin() {
    return user("admin-bim").roles("ADMINISTRADOR");
  }
}
