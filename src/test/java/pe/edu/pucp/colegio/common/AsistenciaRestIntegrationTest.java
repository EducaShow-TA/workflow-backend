package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteCreateRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteRequest;
import pe.edu.pucp.colegio.common.dto.SesionClaseRequest;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionClase;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "asistencia-admin-test", roles = "ADMINISTRADOR")
class AsistenciaRestIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(40_000);
  private static final ZoneId ZONA = ZoneId.systemDefault();
  private static final LocalDate FECHA_FIJA = LocalDate.of(2026, 11, 4);
  private static final LocalTime HORA_INICIO_SESION = LocalTime.of(10, 0);

  @TestConfiguration
  static class RelojFijoConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(
              LocalDateTime.of(FECHA_FIJA, LocalTime.of(10, 5)).atZone(ZONA).toInstant(), ZONA);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @PersistenceContext EntityManager entityManager;

  @Test
  void administradorPuedeCrearSesionDeClase() throws Exception {
    Fixture f = crearFixture();
    SesionClaseRequest request =
            new SesionClaseRequest(
                    f.seccionId(),
                    f.areaCodigo(),
                    f.docenteId(),
                    f.fecha(),
                    LocalTime.of(11, 0),
                    LocalTime.of(11, 45));

    mvc.perform(
                    post("/api/v1/sesiones-clase")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.seccionId").value(f.seccionId()))
            .andExpect(jsonPath("$.areaCodigo").value(f.areaCodigo()))
            .andExpect(jsonPath("$.docenteId").value(f.docenteId()))
            .andExpect(jsonPath("$.horaInicio").value("11:00:00"));
  }

  @Test
  @WithMockUser(username = "docente-asistencia-test", roles = "DOCENTE")
  void docentePuedeVisualizarSesionDeClase() throws Exception {
    Fixture f = crearFixture();

    mvc.perform(get("/api/v1/sesiones-clase/{id}", f.sesionId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(f.sesionId()))
            .andExpect(jsonPath("$.docenteId").value(f.docenteId()));
  }

  @Test
  void puedeFiltrarSesionesPorFechaYSeccion() throws Exception {
    Fixture f = crearFixture();

    mvc.perform(
                    get("/api/v1/sesiones-clase")
                            .param("fecha", f.fecha().toString())
                            .param("seccionId", f.seccionId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(f.sesionId()))
            .andExpect(jsonPath("$[0].fecha").value(f.fecha().toString()));
  }

  @Test
  void registraAsistenciaDeAlumnoPorSesion() throws Exception {
    Fixture f = crearFixture();

    mvc.perform(
                    post("/api/v1/sesiones-clase/{id}/asistencias", f.sesionId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    json(
                                            new AsistenciaEstudianteRequest(
                                                    f.estudianteId(), EstadoAsistencia.PRESENTE))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estudianteId").value(f.estudianteId()))
            .andExpect(jsonPath("$.sesionClaseId").value(f.sesionId()))
            .andExpect(jsonPath("$.estado").value("PRESENTE"));

    mvc.perform(get("/api/v1/sesiones-clase/{id}/asistencias", f.sesionId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].estudianteId").value(f.estudianteId()))
            .andExpect(jsonPath("$[0].estado").value("PRESENTE"));
  }

  @Test
  void rechazaAsistenciaDuplicadaDelMismoAlumnoEnLaSesion() throws Exception {
    Fixture f = crearFixture();
    var request = new AsistenciaEstudianteRequest(f.estudianteId(), EstadoAsistencia.AUSENTE);
    registrarAsistencia(f, request);

    mvc.perform(
                    post("/api/v1/sesiones-clase/{id}/asistencias", f.sesionId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
            .andExpect(status().isConflict());
  }

  @Test
  void ajustaEstadoDeAsistenciaDeAlumno() throws Exception {
    Fixture f = crearFixture();
    registrarAsistencia(
            f, new AsistenciaEstudianteRequest(f.estudianteId(), EstadoAsistencia.AUSENTE));

    mvc.perform(
                    patch(
                            "/api/v1/sesiones-clase/{sesionId}/asistencias/{estudianteId}",
                            f.sesionId(),
                            f.estudianteId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new AsistenciaEstudianteAjusteRequest(EstadoAsistencia.TARDANZA))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("TARDANZA"));
  }

  @Test
  void rechazaSesionConHorarioInvalido() throws Exception {
    Fixture f = crearFixture();
    SesionClaseRequest request =
            new SesionClaseRequest(
                    f.seccionId(),
                    f.areaCodigo(),
                    f.docenteId(),
                    f.fecha(),
                    LocalTime.of(12, 0),
                    LocalTime.of(11, 0));

    mvc.perform(
                    post("/api/v1/sesiones-clase")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
            .andExpect(status().isBadRequest());
  }

  @Test
  void registraAsistenciaDeCualquierDocenteComoAdministrador() throws Exception {
    Fixture f = crearFixture();
    LocalTime entrada = HORA_INICIO_SESION.plusMinutes(3);
    LocalTime salida = entrada.plusHours(7);
    AsistenciaDocenteCreateRequest request =
            new AsistenciaDocenteCreateRequest(
                    f.docenteId(), f.fecha(), EstadoAsistenciaDocente.PRESENTE, entrada, salida, null);

    mvc.perform(
                    post("/api/v1/asistencias-docentes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.docenteId").value(f.docenteId()))
            .andExpect(jsonPath("$.estado").value("PRESENTE"))
            .andExpect(jsonPath("$.horaEntrada").value("10:03:00"));
  }

  @Test
  void visualizaAsistenciaDocenteFiltradaPorFechaYDocente() throws Exception {
    Fixture f = crearFixture();
    int asistenciaId = registrarAsistenciaDocente(f);

    mvc.perform(
                    get("/api/v1/asistencias-docentes")
                            .param("fecha", f.fecha().toString())
                            .param("docenteId", f.docenteId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(asistenciaId))
            .andExpect(jsonPath("$[0].docenteId").value(f.docenteId()));
  }

  @Test
  void ajustaYConsultaAsistenciaDeDocente() throws Exception {
    Fixture f = crearFixture();
    int asistenciaId = registrarAsistenciaDocente(f);

    mvc.perform(
                    patch("/api/v1/asistencias-docentes/{id}", asistenciaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                    json(
                                            new AsistenciaDocenteAjusteRequest(
                                                    EstadoAsistenciaDocente.INASISTENCIA, "Licencia aprobada"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INASISTENCIA"))
            .andExpect(jsonPath("$.motivoAjuste").value("Licencia aprobada"));

    mvc.perform(get("/api/v1/asistencias-docentes/{id}", asistenciaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INASISTENCIA"));
  }

  private Fixture crearFixture() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Persona personaEstudiante = new Persona(suffix, "Ana", "Prueba", "Alumno");
    Persona personaDocente = new Persona("D" + suffix, "Luis", "Prueba", "Docente");
    entityManager.persist(personaEstudiante);
    entityManager.persist(personaDocente);

    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(personaEstudiante);
    entityManager.persist(estudiante);

    Docente docente = new Docente();
    docente.setPersona(personaDocente);
    entityManager.persist(docente);

    Area area = new Area();
    area.setCodigo("REST-" + suffix.substring(0, 8));
    area.setNombre("Área REST de prueba");
    entityManager.persist(area);

    Grado grado = new Grado();
    grado.setNumero(NUMERO_GRADO.getAndIncrement());
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccion = new Seccion();
    seccion.setCodigo("A");
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);

    SesionClase sesion = new SesionClase();
    sesion.setSeccion(seccion);
    sesion.setArea(area);
    sesion.setDocente(docente);
    sesion.setFecha(FECHA_FIJA);
    sesion.setHoraInicio(HORA_INICIO_SESION);
    sesion.setHoraFin(HORA_INICIO_SESION.plusMinutes(45));
    entityManager.persist(sesion);
    entityManager.flush();
    return new Fixture(
            estudiante.getId(),
            docente.getId(),
            area.getCodigo(),
            seccion.getId(),
            sesion.getId(),
            FECHA_FIJA);
  }

  private void registrarAsistencia(Fixture f, AsistenciaEstudianteRequest request)
          throws Exception {
    mvc.perform(
                    post("/api/v1/sesiones-clase/{id}/asistencias", f.sesionId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
            .andExpect(status().isCreated());
  }

  private int registrarAsistenciaDocente(Fixture f) throws Exception {
    MvcResult resultado =
            mvc.perform(
                            post("/api/v1/asistencias-docentes")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            json(
                                                    new AsistenciaDocenteCreateRequest(
                                                            f.docenteId(),
                                                            f.fecha(),
                                                            EstadoAsistenciaDocente.PRESENTE,
                                                            LocalTime.of(8, 0),
                                                            null,
                                                            null))))
                    .andExpect(status().isCreated())
                    .andReturn();
    JsonNode response = objectMapper.readTree(resultado.getResponse().getContentAsString());
    return response.get("id").asInt();
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private record Fixture(
          Integer estudianteId,
          Integer docenteId,
          String areaCodigo,
          Integer seccionId,
          Integer sesionId,
          LocalDate fecha) {}
}