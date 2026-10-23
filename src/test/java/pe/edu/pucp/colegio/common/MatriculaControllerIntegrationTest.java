package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.EstudianteCreateRequest;
import pe.edu.pucp.colegio.common.dto.EstudianteResponse;
import pe.edu.pucp.colegio.common.dto.MatriculaCreateRequest;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;
import pe.edu.pucp.colegio.common.model.Seccion;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "matricula-test", roles = "ADMINISTRADOR")
@Transactional
class MatriculaControllerIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(800);

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @PersistenceContext EntityManager entityManager;

  private Integer ventanaMatriculaId;

  @BeforeEach
  void abrirVentanaDeMatriculaParaLosCasosDeMatricula() {
    PeriodoMatricula ventana = new PeriodoMatricula();
    ventana.setFechaInicio(LocalDate.now().minusDays(1));
    ventana.setFechaLimite(LocalDate.now().plusDays(1));
    entityManager.persist(ventana);
    entityManager.flush();
    ventanaMatriculaId = ventana.getId();
  }

  @Test
  void registraEstudianteLoMatriculaLoRetiraYPuedeMatricularloOtraVez() throws Exception {
    Integer periodoId = crearPeriodo(2);
    EstudianteResponse estudiante = crearEstudiante();

    consultarVacantes(periodoId, 2, 0);
    mvc.perform(
            post("/api/v1/periodos/{periodoId}/matriculas", periodoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new MatriculaCreateRequest(estudiante.estudianteId()))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estudianteId").value(estudiante.estudianteId()))
        .andExpect(jsonPath("$.periodoId").value(periodoId))
        .andExpect(jsonPath("$.estado").value("EN_REGISTRO"));
    consultarVacantes(periodoId, 1, 1);

    mvc.perform(
            delete(
                "/api/v1/periodos/{periodoId}/matriculas/{estudianteId}",
                periodoId,
                estudiante.estudianteId()))
        .andExpect(status().isNoContent());
    consultarVacantes(periodoId, 2, 0);

    mvc.perform(
            delete(
                "/api/v1/periodos/{periodoId}/matriculas/{estudianteId}",
                periodoId,
                estudiante.estudianteId()))
        .andExpect(status().isNotFound());

    mvc.perform(
            post("/api/v1/periodos/{periodoId}/matriculas", periodoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new MatriculaCreateRequest(estudiante.estudianteId()))))
        .andExpect(status().isCreated());
    consultarVacantes(periodoId, 1, 1);
  }

  @Test
  void evitaMatricularDosVecesAlMismoEstudianteEnElPeriodo() throws Exception {
    Integer periodoId = crearPeriodo(2);
    EstudianteResponse estudiante = crearEstudiante();
    String request =
        objectMapper.writeValueAsString(new MatriculaCreateRequest(estudiante.estudianteId()));

    mvc.perform(
            post("/api/v1/periodos/{periodoId}/matriculas", periodoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/periodos/{periodoId}/matriculas", periodoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isConflict());

    consultarVacantes(periodoId, 1, 1);
  }

  @Test
  void impideMatricularCuandoElPeriodoYaNoTieneVacantes() throws Exception {
    Integer periodoId = crearPeriodo(1);
    EstudianteResponse primerEstudiante = crearEstudiante();
    EstudianteResponse segundoEstudiante = crearEstudiante();

    matricular(periodoId, primerEstudiante.estudianteId()).andExpect(status().isCreated());
    matricular(periodoId, segundoEstudiante.estudianteId()).andExpect(status().isConflict());
    consultarVacantes(periodoId, 0, 1);
  }

  @Test
  void noPermiteReducirCapacidadPorDebajoDeLasMatriculasActivas() throws Exception {
    Integer periodoId = crearPeriodo(2);
    EstudianteResponse primero = crearEstudiante();
    EstudianteResponse segundo = crearEstudiante();
    matricular(periodoId, primero.estudianteId()).andExpect(status().isCreated());
    matricular(periodoId, segundo.estudianteId()).andExpect(status().isCreated());
    Integer seccionId = entityManager.find(Periodo.class, periodoId).getSeccion().getId();

    mvc.perform(
            patch("/api/v1/secciones/{id}", seccionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vacantesTotales\":1}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void devuelveNotFoundParaPeriodoOEstudianteInexistentes() throws Exception {
    EstudianteResponse estudiante = crearEstudiante();
    matricular(Integer.MAX_VALUE, estudiante.estudianteId()).andExpect(status().isNotFound());
    matricular(crearPeriodo(1), Integer.MAX_VALUE).andExpect(status().isNotFound());
  }

  @Test
  void confirmaMatriculaEnRegistroYRechazaDobleConfirmacion() throws Exception {
    Integer periodoId = crearPeriodo(1);
    EstudianteResponse estudiante = crearEstudiante();
    matricular(periodoId, estudiante.estudianteId()).andExpect(status().isCreated());

    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/v1/periodos/{periodoId}/matriculas/{estudianteId}/confirmar",
                periodoId,
                estudiante.estudianteId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/v1/periodos/{periodoId}/matriculas/{estudianteId}/confirmar",
                periodoId,
                estudiante.estudianteId()))
        .andExpect(status().isConflict());
  }

  @Test
  void noPermiteRegistrarMatriculaFueraDeLaVentanaVigente() throws Exception {
    Integer periodoId = crearPeriodo(1);
    EstudianteResponse estudiante = crearEstudiante();
    PeriodoMatricula ventana = entityManager.find(PeriodoMatricula.class, ventanaMatriculaId);
    ventana.setFechaInicio(LocalDate.now().minusDays(5));
    ventana.setFechaLimite(LocalDate.now().minusDays(1));
    entityManager.flush();

    mvc.perform(
            post("/api/v1/periodos/{periodoId}/matriculas", periodoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new MatriculaCreateRequest(estudiante.estudianteId()))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No hay un periodo de matrícula vigente"));
  }

  @Test
  void confirmarDevuelveNotFoundSiNoExisteLaMatricula() throws Exception {
    Integer periodoId = crearPeriodo(1);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                "/api/v1/periodos/{periodoId}/matriculas/{estudianteId}/confirmar",
                periodoId,
                Integer.MAX_VALUE))
        .andExpect(status().isNotFound());
  }

  private org.springframework.test.web.servlet.ResultActions matricular(
      Integer periodoId, Integer estudianteId) throws Exception {
    return mvc.perform(
        post("/api/v1/periodos/{periodoId}/matriculas", periodoId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new MatriculaCreateRequest(estudianteId))));
  }

  private void consultarVacantes(Integer periodoId, int disponibles, int matriculasActivas)
      throws Exception {
    mvc.perform(get("/api/v1/periodos/{periodoId}/vacantes", periodoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vacantesDisponibles").value(disponibles))
        .andExpect(jsonPath("$.matriculasActivas").value(matriculasActivas));
  }

  private EstudianteResponse crearEstudiante() throws Exception {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String dni =
        String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000));
    String dniApoderado =
        String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000));
    EstudianteCreateRequest request =
        new EstudianteCreateRequest(
            "alumno-" + suffix,
            "ClaveSegura123",
            "DNI",
            dni,
            "Estudiante",
            "Prueba",
            "Matricula",
            LocalDate.of(2012, 5, 10),
            "999999999",
            "NO_IDENTIFICADO",
            null,
            null,
            null,
            java.util.List.of(
                new EstudianteCreateRequest.ApoderadoCreateRequest(
                    "DNI",
                    dniApoderado,
                    "Apoderado",
                    "Prueba",
                    "Matricula",
                    "999888777",
                    null,
                    "MADRE",
                    true)));

    String response =
        mvc.perform(
                post("/api/v1/estudiantes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rol").value("ALUMNO"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(response, EstudianteResponse.class);
  }

  private Integer crearPeriodo(Integer vacantes) {
    Grado grado = new Grado();
    grado.setNumero(NUMERO_GRADO.getAndIncrement());
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);

    Seccion seccion = new Seccion();
    seccion.setCodigo("A");
    seccion.setVacantesTotales(vacantes);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    entityManager.persist(seccion);

    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(LocalDate.of(2026, 3, 1));
    periodo.setFechaFin(LocalDate.of(2026, 12, 20));
    periodo.setEstado(EstadoPeriodo.PLANIFICADO);
    entityManager.persist(periodo);
    entityManager.flush();
    return periodo.getId();
  }
}
