package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@WithMockUser(username = "vacantes-test", roles = "ADMINISTRADOR")
@Transactional
class VacantesPeriodoIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(100);

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @PersistenceContext EntityManager entityManager;

  @Test
  void calculaVacantesConMatriculasActivasDelPeriodoSolicitado() throws Exception {
    Seccion seccion = crearSeccion("30");
    Periodo periodo =
        crearPeriodo(seccion, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31));
    agregarMatricula(periodo, EstadoMatricula.CONFIRMADA, true);
    agregarMatricula(periodo, EstadoMatricula.EN_REGISTRO, true);
    agregarMatricula(periodo, EstadoMatricula.FINALIZADA, true);
    agregarMatricula(periodo, EstadoMatricula.RETIRADA, true);
    agregarMatricula(periodo, EstadoMatricula.CONFIRMADA, false);
    entityManager.flush();

    mvc.perform(get("/api/v1/periodos/{periodoId}/vacantes", periodo.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodoId").value(periodo.getId()))
        .andExpect(jsonPath("$.numeroGrado").value(seccion.getGrado().getNumero()))
        .andExpect(jsonPath("$.nivelEducativo").value("PRIMARIA"))
        .andExpect(jsonPath("$.seccion").value("A"))
        .andExpect(jsonPath("$.fechaInicio").value("2026-03-01"))
        .andExpect(jsonPath("$.fechaFin").value("2026-07-31"))
        .andExpect(jsonPath("$.vacantesTotales").value(30))
        .andExpect(jsonPath("$.matriculasActivas").value(3))
        .andExpect(jsonPath("$.vacantesDisponibles").value(27))
        .andExpect(jsonPath("$.hayVacantes").value(true));
  }

  @Test
  void conteoDeMatriculasQuedaLimitadoAlPeriodoYDevuelveSinVacantesCuandoSeCompleta()
      throws Exception {
    Seccion seccion = crearSeccion("2");
    Periodo primerPeriodo =
        crearPeriodo(seccion, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 7, 31));
    Periodo segundoPeriodo =
        crearPeriodo(seccion, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 20));
    agregarMatricula(primerPeriodo, EstadoMatricula.CONFIRMADA, true);
    agregarMatricula(segundoPeriodo, EstadoMatricula.CONFIRMADA, true);
    agregarMatricula(segundoPeriodo, EstadoMatricula.EN_REGISTRO, true);
    entityManager.flush();

    mvc.perform(get("/api/v1/periodos/{periodoId}/vacantes", segundoPeriodo.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matriculasActivas").value(2))
        .andExpect(jsonPath("$.vacantesDisponibles").value(0))
        .andExpect(jsonPath("$.hayVacantes").value(false));
  }

  @Test
  void periodoInexistenteDevuelveNotFound() throws Exception {
    mvc.perform(get("/api/v1/periodos/{periodoId}/vacantes", Integer.MAX_VALUE))
        .andExpect(status().isNotFound());
  }

  @Test
  void creaPeriodoPorApiYConsultaSusVacantesLibres() throws Exception {
    Seccion seccion = crearSeccion("6");
    entityManager.flush();

    String request =
        "{\"gradoId\":"
            + seccion.getGrado().getId()
            + ",\"seccionId\":"
            + seccion.getId()
            + ",\"fechaInicio\":\"2027-03-01\",\"fechaFin\":\"2027-12-20\"}";

    String response =
        mvc.perform(
                post("/api/v1/periodos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.gradoId").value(seccion.getGrado().getId()))
            .andExpect(jsonPath("$.seccionId").value(seccion.getId()))
            .andExpect(jsonPath("$.estado").value("PLANIFICADO"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode periodoCreado = objectMapper.readTree(response);

    mvc.perform(get("/api/v1/periodos/{periodoId}/vacantes", periodoCreado.get("id").asInt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vacantesTotales").value(6))
        .andExpect(jsonPath("$.matriculasActivas").value(0))
        .andExpect(jsonPath("$.vacantesDisponibles").value(6))
        .andExpect(jsonPath("$.hayVacantes").value(true));
  }

  private Seccion crearSeccion(String vacantes) {
    Grado grado = new Grado();
    grado.setNumero(NUMERO_GRADO.getAndIncrement());
    grado.setNivelEducativo(NivelEducativo.PRIMARIA);

    Seccion seccion = new Seccion();
    seccion.setCodigo("A");
    seccion.setVacantesTotales(vacantes);
    grado.agregarSeccion(seccion);

    entityManager.persist(grado);
    entityManager.persist(seccion);
    return seccion;
  }

  private Periodo crearPeriodo(Seccion seccion, LocalDate inicio, LocalDate fin) {
    Periodo periodo = new Periodo();
    periodo.setGrado(seccion.getGrado());
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(inicio);
    periodo.setFechaFin(fin);
    periodo.setEstado(EstadoPeriodo.PLANIFICADO);
    entityManager.persist(periodo);
    return periodo;
  }

  private void agregarMatricula(Periodo periodo, EstadoMatricula estado, boolean activa) {
    String dni = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    Persona persona = new Persona(dni, "Estudiante", "Prueba", "Vacantes");
    entityManager.persist(persona);

    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);

    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(LocalDate.of(2026, 2, 15));
    matricula.setEstado(estado);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    matricula.setActive(activa);
    entityManager.persist(matricula);
  }
}
