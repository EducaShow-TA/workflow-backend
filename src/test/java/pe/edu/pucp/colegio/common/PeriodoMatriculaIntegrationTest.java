package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaRequest;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.PeriodoMatriculaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "periodo-matricula-test", roles = "ADMINISTRADOR")
@Transactional
class PeriodoMatriculaIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(5000);

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Autowired PeriodoMatriculaRepository periodos;

  @PersistenceContext EntityManager entityManager;

  @Test
  void configuraVentanasYExponeSusEstadosCalculados() throws Exception {
    long ventanasAntes = periodos.countByActiveTrue();
    crear(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    crear(LocalDate.now(), LocalDate.now().plusDays(2))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("VIGENTE"));

    mvc.perform(get("/api/v1/periodos-matricula"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(ventanasAntes + 2));
  }

  @Test
  void rechazaLimiteIgualOAnteriorAlInicio() throws Exception {
    crear(LocalDate.now(), LocalDate.now()).andExpect(status().isUnprocessableEntity());
    crear(LocalDate.now().plusDays(2), LocalDate.now().plusDays(1))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaFechaDeInicioAnteriorAHoy() throws Exception {
    crear(LocalDate.now().minusDays(1), LocalDate.now().plusDays(2))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaInicioDemasiadoLejanoDelAnhioEscolarDeLaFechaLimite() throws Exception {
    int anhio = LocalDate.now().getYear();
    crear(LocalDate.of(anhio + 2, 1, 10), LocalDate.of(anhio + 4, 1, 20))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "El inicio del período no puede ser más de un año calendario anterior al año escolar de la fecha límite"));
  }

  @Test
  void actualizarVentanaValidaFechasPasadasOrdenYCruces() throws Exception {
    LocalDate hoy = LocalDate.now();
    int primero = crearYObtenerId(hoy.plusDays(10), hoy.plusDays(20));
    int segundo = crearYObtenerId(hoy.plusDays(30), hoy.plusDays(40));

    actualizar(primero, hoy.plusDays(12), hoy.plusDays(22))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaInicio").value(hoy.plusDays(12).toString()));
    actualizar(primero, hoy.minusDays(1), hoy.plusDays(22))
        .andExpect(status().isUnprocessableEntity());
    actualizar(primero, hoy.plusDays(24), hoy.plusDays(23))
        .andExpect(status().isUnprocessableEntity());
    actualizar(primero, hoy.plusDays(32), hoy.plusDays(35)).andExpect(status().isConflict());
    mvc.perform(get("/api/v1/periodos-matricula/{id}", primero))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaInicio").value(hoy.plusDays(12).toString()));
  }

  @Test
  void restringeConfiguracionAAdministradores() throws Exception {
    mvc.perform(
            post("/api/v1/periodos-matricula")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("docente")
                        .roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaInicio\":\"2026-01-01\",\"fechaLimite\":\"2026-01-10\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void desactivarVentanaLaRetiraDelListadoYConsulta() throws Exception {
    String body =
        crear(LocalDate.now(), LocalDate.now().plusDays(1))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = objectMapper.readTree(body).get("id").asInt();

    mvc.perform(delete("/api/v1/periodos-matricula/{id}", id)).andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/periodos-matricula/{id}", id)).andExpect(status().isNotFound());
  }

  @Test
  void periodoVigenteSoloPermiteMoverSuFechaLimite() throws Exception {
    LocalDate hoy = LocalDate.now();
    LocalDate inicio = hoy.minusDays(5);
    int id = guardarVentana(inicio, hoy.plusDays(3));

    actualizar(id, inicio, hoy.plusDays(10))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaInicio").value(inicio.toString()))
        .andExpect(jsonPath("$.fechaLimite").value(hoy.plusDays(10).toString()))
        .andExpect(jsonPath("$.estado").value("VIGENTE"));
    actualizar(id, hoy, hoy.plusDays(10))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message")
                .value("El periodo ya inició; su fecha de inicio no se puede modificar"));
    actualizar(id, inicio, hoy.minusDays(1)).andExpect(status().isUnprocessableEntity());
    actualizar(id, inicio, hoy).andExpect(status().isOk());
  }

  @Test
  void noDesactivaUnPeriodoVencidoConMatriculasPeroSiUnoSinMatriculas() throws Exception {
    LocalDate inicio = LocalDate.of(2001, 1, 1);
    int conMatriculas = guardarVentana(inicio, inicio.plusDays(30));
    int sinMatriculas = guardarVentana(LocalDate.of(2002, 1, 1), LocalDate.of(2002, 1, 31));
    agregarMatricula(inicio.plusDays(10));

    mvc.perform(delete("/api/v1/periodos-matricula/{id}", conMatriculas))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/v1/periodos-matricula/{id}", conMatriculas))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tieneMatriculas").value(true));
    mvc.perform(delete("/api/v1/periodos-matricula/{id}", sinMatriculas))
        .andExpect(status().isNoContent());
  }

  private int guardarVentana(LocalDate inicio, LocalDate limite) {
    PeriodoMatricula ventana = new PeriodoMatricula();
    ventana.setFechaInicio(inicio);
    ventana.setFechaLimite(limite);
    entityManager.persist(ventana);
    entityManager.flush();
    return ventana.getId();
  }

  private void agregarMatricula(LocalDate fechaMatricula) {
    Grado grado = new Grado();
    grado.setNumero(NUMERO_GRADO.getAndIncrement());
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccion = new Seccion();
    seccion.setCodigo("A");
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    entityManager.persist(seccion);

    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(fechaMatricula.plusMonths(2));
    periodo.setFechaFin(fechaMatricula.plusMonths(11));
    periodo.setEstado(EstadoPeriodo.PLANIFICADO);
    entityManager.persist(periodo);

    String dni = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    Persona persona = new Persona(dni, "Estudiante", "Prueba", "Historial");
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);

    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(fechaMatricula);
    matricula.setEstado(EstadoMatricula.CONFIRMADA);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);
    entityManager.flush();
  }

  private org.springframework.test.web.servlet.ResultActions crear(
      LocalDate inicio, LocalDate limite) throws Exception {
    return mvc.perform(
        post("/api/v1/periodos-matricula")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PeriodoMatriculaRequest(inicio, limite))));
  }

  private int crearYObtenerId(LocalDate inicio, LocalDate limite) throws Exception {
    String body =
        crear(inicio, limite)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("id").asInt();
  }

  private org.springframework.test.web.servlet.ResultActions actualizar(
      int id, LocalDate inicio, LocalDate limite) throws Exception {
    return mvc.perform(
        put("/api/v1/periodos-matricula/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PeriodoMatriculaRequest(inicio, limite))));
  }
}
