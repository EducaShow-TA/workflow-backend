package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichaEstudianteIntegrationTest {

  private static final String RUTA = "/api/v1/estudiantes/mi-ficha";

  @Autowired MockMvc mvc;
  @PersistenceContext EntityManager entityManager;

  private String sufijo;
  private Grado grado;
  private Seccion seccion;

  @BeforeEach
  void prepararGrado() {
    sufijo = UUID.randomUUID().toString().substring(0, 8);
    grado = new Grado();
    grado.setNumero(Math.abs(sufijo.hashCode()) + 1);
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    seccion = new Seccion();
    seccion.setCodigo("F-" + sufijo);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
  }

  @Test
  void alumnoObtieneSoloSusMatriculasConfirmadasDeLaMasRecienteALaMasAntigua() throws Exception {
    Persona persona = persona("E", "Valeria");
    Estudiante estudiante = estudiante(persona);
    String usuario = cuenta(persona, Rol.ALUMNO, true);
    Periodo anterior = periodo(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 12, 19));
    Periodo actual = periodo(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 12, 18));
    Periodo siguiente = periodo(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 12, 17));
    matricular(estudiante, anterior, EstadoMatricula.CONFIRMADA);
    matricular(estudiante, actual, EstadoMatricula.CONFIRMADA);
    matricular(estudiante, siguiente, EstadoMatricula.EN_REGISTRO);
    entityManager.flush();
    entityManager.clear();

    mvc.perform(get(RUTA).with(user(usuario).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estudianteId").value(estudiante.getId()))
        .andExpect(jsonPath("$.nombreCompleto").value(persona.getNombreCompleto()))
        .andExpect(jsonPath("$.periodos", hasSize(2)))
        .andExpect(jsonPath("$.periodos[0].periodoId").value(actual.getId()))
        .andExpect(jsonPath("$.periodos[0].gradoNumero").value(grado.getNumero()))
        .andExpect(jsonPath("$.periodos[0].seccion").value(seccion.getCodigo()))
        .andExpect(jsonPath("$.periodos[0].fechaInicio").value("2026-03-02"))
        .andExpect(jsonPath("$.periodos[1].periodoId").value(anterior.getId()));
  }

  @Test
  void alumnoSinMatriculaConfirmadaRecibeListaDePeriodosVacia() throws Exception {
    Persona persona = persona("E", "Bruno");
    estudiante(persona);
    String usuario = cuenta(persona, Rol.ALUMNO, true);
    entityManager.flush();
    entityManager.clear();

    mvc.perform(get(RUTA).with(user(usuario).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodos", hasSize(0)));
  }

  @Test
  void cuentaDeAlumnoSinRegistroDeEstudianteEsRechazada() throws Exception {
    String usuario = cuenta(persona("E", "Iris"), Rol.ALUMNO, true);
    entityManager.flush();

    mvc.perform(get(RUTA).with(user(usuario).roles("ALUMNO")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("No existe un registro de estudiante activo"));
  }

  @Test
  void cuentaInactivaODesconocidaEsRechazada() throws Exception {
    Persona persona = persona("E", "Tomás");
    estudiante(persona);
    String inactiva = cuenta(persona, Rol.ALUMNO, false);
    entityManager.flush();

    mvc.perform(get(RUTA).with(user(inactiva).roles("ALUMNO")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("La cuenta no corresponde a un alumno activo"));
    mvc.perform(get(RUTA).with(user("no-existe-" + sufijo).roles("ALUMNO")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("La cuenta no corresponde a un alumno activo"));
  }

  @Test
  void otrosRolesNoPuedenConsultarLaFicha() throws Exception {
    Persona persona = persona("P", "Rosa");
    String usuario = cuenta(persona, Rol.APODERADO, true);
    entityManager.flush();

    mvc.perform(get(RUTA).with(user(usuario).roles("APODERADO"))).andExpect(status().isForbidden());
    mvc.perform(get(RUTA).with(user("admin-" + sufijo).roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
  }

  private Persona persona(String prefijo, String nombre) {
    Persona persona =
        new Persona(
            prefijo + UUID.randomUUID().toString().substring(0, 7), nombre, "Ruiz", "Pardo");
    entityManager.persist(persona);
    return persona;
  }

  private Estudiante estudiante(Persona persona) {
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);
    return estudiante;
  }

  private String cuenta(Persona persona, Rol rol, boolean activa) {
    String usuario = rol.name().toLowerCase() + "-" + persona.getDni();
    CuentaUsuario cuenta = new CuentaUsuario(usuario, "hash", rol, persona);
    cuenta.setActive(activa);
    entityManager.persist(cuenta);
    return usuario;
  }

  private Periodo periodo(LocalDate desde, LocalDate hasta) {
    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(desde);
    periodo.setFechaFin(hasta);
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    entityManager.persist(periodo);
    return periodo;
  }

  private void matricular(Estudiante estudiante, Periodo periodo, EstadoMatricula estado) {
    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(periodo.getFechaInicio());
    matricula.setEstado(estado);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);
  }
}
