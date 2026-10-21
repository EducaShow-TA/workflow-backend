package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
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

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:vacantes-transacciones;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser(username = "vacantes-transacciones-test", roles = "ADMINISTRADOR")
class VacantesPeriodoTransaccionesIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(500);

  @Autowired MockMvc mvc;

  @Autowired PlatformTransactionManager transactionManager;

  @PersistenceContext EntityManager entityManager;

  @Test
  void confirmaDisminucionDeVacantesYRevierteAltasNoConfirmadas() throws Exception {
    Integer periodoId =
        enTransaccion(
            status -> {
              Periodo periodo = crearPeriodo();
              agregarUsuarioMatriculado(periodo, EstadoMatricula.CONFIRMADA);
              entityManager.flush();
              return periodo.getId();
            });

    try {
      consultarVacantes(periodoId, 2, 1);

      Integer segundaMatriculaId =
          enTransaccion(
              status -> {
                Periodo periodo = entityManager.find(Periodo.class, periodoId);
                Matricula matricula =
                    agregarUsuarioMatriculado(periodo, EstadoMatricula.EN_REGISTRO);
                entityManager.flush();
                return matricula.getId();
              });
      consultarVacantes(periodoId, 1, 2);

      enTransaccion(
          status -> {
            Periodo periodo = entityManager.find(Periodo.class, periodoId);
            agregarUsuarioMatriculado(periodo, EstadoMatricula.CONFIRMADA);
            entityManager.flush();
            status.setRollbackOnly();
            return null;
          });
      consultarVacantes(periodoId, 1, 2);

      enTransaccion(
          status -> {
            Matricula matricula = entityManager.find(Matricula.class, segundaMatriculaId);
            matricula.setEstado(EstadoMatricula.RETIRADA);
            return null;
          });
      consultarVacantes(periodoId, 2, 1);
    } finally {
      limpiarPeriodo(periodoId);
    }
  }

  private Periodo crearPeriodo() {
    Grado grado = new Grado();
    grado.setNumero(NUMERO_GRADO.getAndIncrement());
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);

    Seccion seccion = new Seccion();
    seccion.setCodigo("A");
    seccion.setVacantesTotales("3");
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);
    entityManager.persist(seccion);

    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(LocalDate.of(2026, 3, 1));
    periodo.setFechaFin(LocalDate.of(2026, 12, 20));
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    entityManager.persist(periodo);
    return periodo;
  }

  private Matricula agregarUsuarioMatriculado(Periodo periodo, EstadoMatricula estado) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Persona persona = new Persona();
    persona.setDni(suffix);
    persona.setNombre("Estudiante");
    persona.setApellidoPaterno("Transaccional");
    persona.setApellidoMaterno("Prueba");
    entityManager.persist(persona);

    CuentaUsuario usuario = new CuentaUsuario();
    usuario.setUsername("alumno-" + suffix);
    usuario.setPassword("hash-de-prueba");
    usuario.setRol(Rol.ALUMNO);
    usuario.setPersona(persona);
    entityManager.persist(usuario);

    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);

    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(LocalDate.of(2026, 2, 15));
    matricula.setEstado(estado);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    entityManager.persist(matricula);
    return matricula;
  }

  private void consultarVacantes(Integer periodoId, int disponibles, int matriculasActivas)
      throws Exception {
    mvc.perform(get("/api/v1/periodos/{periodoId}/vacantes", periodoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vacantesDisponibles").value(disponibles))
        .andExpect(jsonPath("$.matriculasActivas").value(matriculasActivas));
  }

  private void limpiarPeriodo(Integer periodoId) {
    enTransaccion(
        status -> {
          Periodo periodo = entityManager.find(Periodo.class, periodoId);
          if (periodo == null) {
            return null;
          }
          Seccion seccion = periodo.getSeccion();
          Grado grado = periodo.getGrado();
          var matriculas =
              entityManager
                  .createQuery(
                      "select m from Matricula m where m.periodo.id = :periodoId", Matricula.class)
                  .setParameter("periodoId", periodoId)
                  .getResultList();

          for (Matricula matricula : matriculas) {
            Estudiante estudiante = matricula.getEstudiante();
            Persona persona = estudiante.getPersona();
            CuentaUsuario usuario =
                entityManager
                    .createQuery(
                        "select u from CuentaUsuario u where u.persona.id = :personaId",
                        CuentaUsuario.class)
                    .setParameter("personaId", persona.getId())
                    .getSingleResult();
            entityManager.remove(matricula);
            entityManager.remove(estudiante);
            entityManager.remove(usuario);
            entityManager.remove(persona);
          }

          entityManager.remove(periodo);
          entityManager.remove(seccion);
          entityManager.remove(grado);
          return null;
        });
  }

  private <T> T enTransaccion(TransactionCallback<T> callback) {
    return new TransactionTemplate(transactionManager).execute(callback);
  }
}
