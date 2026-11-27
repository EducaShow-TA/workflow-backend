package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.PeriodoMatriculaRepository;
import pe.edu.pucp.colegio.common.service.PeriodoMatriculaService;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "periodo-matricula-test", roles = "ADMINISTRADOR")
@Transactional
class PeriodoMatriculaIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(5000);

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Autowired PeriodoMatriculaRepository periodos;

  @Autowired PeriodoMatriculaService periodosMatricula;

  @PersistenceContext EntityManager entityManager;

  @Test
  void listarNoCreaAperturasAutomaticamente() throws Exception {
    int anhio = LocalDate.now().getYear();
    mvc.perform(get("/api/v1/periodos-matricula"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    assertEquals(0, periodos.countByActiveTrue());
    org.junit.jupiter.api.Assertions.assertFalse(
        periodosMatricula.hayPeriodoVigenteParaAnhio(anhio));
  }

  @Test
  void noVuelveAAbrirUnaVentanaAnualDesactivada() throws Exception {
    int anhio = LocalDate.now().getYear();
    int id = crearYObtenerIdPorAnhio(anhio);

    mvc.perform(delete("/api/v1/periodos-matricula/{id}", id)).andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/periodos-matricula"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    org.junit.jupiter.api.Assertions.assertFalse(
        periodosMatricula.hayPeriodoVigenteParaAnhio(anhio));
  }

  @Test
  void abrirElAnioSiguienteCierraLaMatriculaDelAnioActualAlCerrarSusActas() throws Exception {
    LocalDate hoy = LocalDate.now();
    int anhio = hoy.getYear();
    LocalDate inicioClases = LocalDate.of(anhio, 1, 2);
    crearPeriodoAcademico(inicioClases);
    long ventanasAntes = periodos.countByActiveTrue();
    crearPorAnhio(anhio)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.anhioEscolar").value(anhio))
        .andExpect(jsonPath("$.estado").value("VIGENTE"));
    crearPorAnhio(anhio + 1)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.anhioEscolar").value(anhio + 1))
        .andExpect(jsonPath("$.fechaInicio").value(hoy.toString()))
        .andExpect(jsonPath("$.fechaLimite").value("2100-12-31"))
        .andExpect(jsonPath("$.estado").value("VIGENTE"));

    org.junit.jupiter.api.Assertions.assertFalse(
        periodosMatricula.hayPeriodoVigenteParaAnhio(anhio));
    org.junit.jupiter.api.Assertions.assertTrue(
        periodosMatricula.hayPeriodoVigenteParaAnhio(anhio + 1));

    mvc.perform(get("/api/v1/periodos-matricula"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(ventanasAntes + 2))
        .andExpect(jsonPath("$[?(@.anhioEscolar == " + anhio + ")].estado").value("VENCIDO"))
        .andExpect(jsonPath("$[?(@.anhioEscolar == " + (anhio + 1) + ")].estado").value("VIGENTE"));
  }

  @Test
  void rechazaUnAnioEscolarFueraDelRangoPermitido() throws Exception {
    crearPorAnhio(1999).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void aperturaIgnoraFechasDeCierreYPermaneceActivaHastaDesactivacion() throws Exception {
    int anhio = LocalDate.now().getYear();
    crear(LocalDate.now().minusDays(1), LocalDate.now().plusDays(2))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.anhioEscolar").value(anhio))
        .andExpect(jsonPath("$.fechaInicio").value(LocalDate.now().toString()))
        .andExpect(jsonPath("$.fechaLimite").value("2100-12-31"))
        .andExpect(jsonPath("$.estado").value("VIGENTE"));
  }

  @Test
  void laVentanaAbiertaConLasClasesEnCursoEsParaElAnhioSiguiente() throws Exception {
    int anhio = LocalDate.now().getYear() + 5;
    crearPeriodoAcademico(LocalDate.of(anhio, 3, 2));

    crear(LocalDate.of(anhio, 11, 16), LocalDate.of(anhio, 12, 31))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.anhioEscolar").value(anhio + 1))
        .andExpect(jsonPath("$.fechaInicio").value(LocalDate.now().toString()))
        .andExpect(jsonPath("$.estado").value("VIGENTE"));
    assertEquals(anhio, periodosMatricula.anhioEscolar(LocalDate.of(anhio, 1, 5)));
    assertEquals(anhio, periodosMatricula.anhioEscolar(LocalDate.of(anhio, 3, 1)));
    assertEquals(anhio + 1, periodosMatricula.anhioEscolar(LocalDate.of(anhio, 3, 2)));
    assertEquals(anhio + 2, periodosMatricula.anhioEscolar(LocalDate.of(anhio + 2, 11, 1)));
  }

  @Test
  void rechazaConfigurarMatriculaParaUnAnioAnterior() throws Exception {
    crearPorAnhio(LocalDate.now().getYear() - 1)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message").value("No se puede configurar matrícula para un año anterior"));
  }

  @Test
  void actualizarVentanaAnualMantieneLaUnicidadPorAnioEscolar() throws Exception {
    int anhio = LocalDate.now().getYear();
    int primero = crearYObtenerIdPorAnhio(anhio);
    int segundo = crearYObtenerIdPorAnhio(anhio + 1);

    actualizarPorAnhio(primero, anhio)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anhioEscolar").value(anhio));
    actualizarPorAnhio(primero, anhio + 1).andExpect(status().isUnprocessableEntity());
    mvc.perform(get("/api/v1/periodos-matricula/{id}", primero))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anhioEscolar").value(anhio));
    mvc.perform(get("/api/v1/periodos-matricula/{id}", segundo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anhioEscolar").value(anhio + 1));
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
    mvc.perform(get("/api/v1/periodos-matricula"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void periodoVigenteConservaSuFechaDeInicioYFinAnual() throws Exception {
    LocalDate hoy = LocalDate.now();
    int anhio = hoy.getYear();
    crearPeriodoAcademico(LocalDate.of(anhio, 1, 2));
    int id = crearYObtenerIdPorAnhio(anhio);

    actualizarPorAnhio(id, anhio)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaInicio").value(hoy.toString()))
        .andExpect(jsonPath("$.fechaLimite").value("2100-12-31"))
        .andExpect(jsonPath("$.estado").value("VIGENTE"));
    actualizarPorAnhio(id, anhio + 1)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message")
                .value("El año escolar de un periodo vigente no se puede modificar"));
  }

  @Test
  private org.springframework.test.web.servlet.ResultActions crear(
      LocalDate inicio, LocalDate limite) throws Exception {
    int anhio = periodosMatricula.anhioEscolar(inicio);
    prepararRequisitosApertura(anhio);
    return mvc.perform(
        post("/api/v1/periodos-matricula")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(
                    new PeriodoMatriculaRequest(
                        inicio,
                        limite,
                        anhio,
                        new BigDecimal("300.00"),
                        new BigDecimal("500.00")))));
  }

  private org.springframework.test.web.servlet.ResultActions crearPorAnhio(int anhio)
      throws Exception {
    if (anhio >= LocalDate.now().getYear() && anhio <= 2100) {
      prepararRequisitosApertura(anhio);
    }
    return mvc.perform(
        post("/api/v1/periodos-matricula")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(
                    new PeriodoMatriculaRequest(
                        null, null, anhio, new BigDecimal("300.00"), new BigDecimal("500.00")))));
  }

  private int crearYObtenerIdPorAnhio(int anhio) throws Exception {
    String body =
        crearPorAnhio(anhio)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(body).get("id").asInt();
  }

  private org.springframework.test.web.servlet.ResultActions actualizarPorAnhio(int id, int anhio)
      throws Exception {
    return mvc.perform(
        put("/api/v1/periodos-matricula/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(
                    new PeriodoMatriculaRequest(
                        null, null, anhio, new BigDecimal("300.00"), new BigDecimal("500.00")))));
  }

  private void prepararRequisitosApertura(int anhio) {
    var activas =
        entityManager
            .createQuery("select s from Seccion s where s.active = true", Seccion.class)
            .getResultList();
    if (activas.isEmpty()) {
      Grado grado = new Grado();
      grado.setNumero(NUMERO_GRADO.getAndIncrement());
      grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
      Seccion seccion = new Seccion();
      seccion.setCodigo("A");
      seccion.setVacantesTotales(30);
      grado.agregarSeccion(seccion);
      entityManager.persist(grado);
      entityManager.persist(seccion);
      activas = java.util.List.of(seccion);
    }
    cerrarActasDelAnio(anhio - 1);
    for (Seccion seccion : activas) {
      asegurarPeriodoAcademico(seccion, anhio - 1, true);
      asegurarPeriodoAcademico(seccion, anhio, false);
    }
    entityManager.flush();
  }

  private void cerrarActasDelAnio(int anhio) {
    var periodosDelAnio =
        entityManager
            .createQuery(
                "select p from Periodo p where p.fechaInicio <= :fin and p.fechaFin >= :inicio",
                Periodo.class)
            .setParameter("inicio", LocalDate.of(anhio, 1, 1))
            .setParameter("fin", LocalDate.of(anhio, 12, 31))
            .getResultList();
    periodosDelAnio.forEach(periodo -> periodo.setActasCerradas(true));
  }

  private void crearPeriodoAcademico(LocalDate inicioClases) {
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
    periodo.setFechaInicio(inicioClases);
    periodo.setFechaFin(inicioClases.plusMonths(9));
    periodo.setEstado(EstadoPeriodo.PLANIFICADO);
    entityManager.persist(periodo);
    entityManager.flush();
  }

  private void asegurarPeriodoAcademico(Seccion seccion, int anhio, boolean actasCerradas) {
    LocalDate inicio = LocalDate.of(anhio, 3, 2);
    LocalDate fin = LocalDate.of(anhio, 12, 31);
    var existente =
        entityManager
            .createQuery(
                "select p from Periodo p where p.seccion.id = :seccionId"
                    + " and p.fechaInicio = :inicio and p.fechaFin = :fin",
                Periodo.class)
            .setParameter("seccionId", seccion.getId())
            .setParameter("inicio", inicio)
            .setParameter("fin", fin)
            .getResultList();
    Periodo periodo;
    if (existente.isEmpty()) {
      periodo = new Periodo();
      periodo.setGrado(seccion.getGrado());
      periodo.setSeccion(seccion);
      periodo.setFechaInicio(inicio);
      periodo.setFechaFin(fin);
      periodo.setEstado(EstadoPeriodo.PLANIFICADO);
      entityManager.persist(periodo);
    } else {
      periodo = existente.get(0);
    }
    if (actasCerradas) {
      periodo.setActasCerradas(true);
    }
  }
}
