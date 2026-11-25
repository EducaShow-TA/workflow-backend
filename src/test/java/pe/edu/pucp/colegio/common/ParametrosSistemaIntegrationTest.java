package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.config.RelojSistema;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest(properties = {"app.demo.fecha-permitida=true", "app.pagos.simulado.enabled=true"})
@AutoConfigureMockMvc
@Transactional
class ParametrosSistemaIntegrationTest {

  private static final String RUTA = "/api/v1/parametros-sistema";
  private static final String CONTRASENIA = "ClaveDemo123!";
  private static final String FECHA_DEMO =
      "{\"fecha\":\"2026-04-01T07:50:00\",\"motivo\":\"Demostración del flujo de bimestres\","
          + "\"contrasenia\":\""
          + CONTRASENIA
          + "\"}";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired RelojSistema reloj;
  @Autowired Clock clock;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired PasswordEncoder encoder;

  @BeforeEach
  void crearAdministrador() {
    cuentas.save(
        new CuentaUsuario("admin-param", encoder.encode(CONTRASENIA), Rol.ADMINISTRADOR, null));
  }

  @AfterEach
  void volverAFechaReal() {
    reloj.ajustar(Duration.ZERO);
  }

  @Test
  void elAdministradorSimulaLaFechaYTodoElSistemaLaUsa() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FECHA_DEMO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaSimulada").value(true))
        .andExpect(jsonPath("$.fechaSistema", startsWith("2026-04-01T07:5")))
        .andExpect(jsonPath("$.motivoFecha").value("Demostración del flujo de bimestres"))
        .andExpect(jsonPath("$.fechaSimuladaPor").value("admin-param"));

    assertEquals(LocalDate.of(2026, 4, 1), LocalDate.now(clock));
    mvc.perform(get(RUTA + "/estado").with(user("docente-param").roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaSimulada").value(true))
        .andExpect(jsonPath("$.fechaSistema", startsWith("2026-04-01")));
  }

  @Test
  void laAuditoriaRegistraElCambioConLaHoraReal() throws Exception {
    LocalDateTime antes = LocalDateTime.now().minusMinutes(1);
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FECHA_DEMO))
        .andExpect(status().isOk());

    LocalDateTime registrada =
        jdbc.queryForObject(
            "select fecha_hora from registros_auditoria where accion='CAMBIAR_FECHA_SISTEMA' order by id desc limit 1",
            LocalDateTime.class);
    String descripcion =
        jdbc.queryForObject(
            "select descripcion from registros_auditoria where accion='CAMBIAR_FECHA_SISTEMA' order by id desc limit 1",
            String.class);
    assertTrue(registrada.isAfter(antes));
    assertTrue(descripcion.contains("01/04/2026 07:50"));
    assertTrue(descripcion.contains("Motivo: Demostración del flujo de bimestres"));
  }

  @Test
  void volverALaFechaRealQuitaLaSimulacion() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FECHA_DEMO))
        .andExpect(status().isOk());

    mvc.perform(delete(RUTA + "/fecha").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaSimulada").value(false))
        .andExpect(jsonPath("$.motivoFecha").doesNotExist());
    assertEquals(LocalDate.now(reloj.real()), LocalDate.now(clock));

    mvc.perform(delete(RUTA + "/fecha").with(admin()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("El sistema ya usa la fecha real"));
  }

  @Test
  void exigeUnMotivoSuficiente() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fecha\":\"2026-04-01T07:50:00\",\"motivo\":\"demo\",\"contrasenia\":\""
                        + CONTRASENIA
                        + "\"}"))
        .andExpect(status().isBadRequest());
    assertEquals(Duration.ZERO, reloj.desfase());
  }

  @Test
  void rechazaFechasFueraDeRango() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fecha\":\"1999-04-01T07:50:00\",\"motivo\":\"Demostración del sistema\","
                        + "\"contrasenia\":\""
                        + CONTRASENIA
                        + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Elige una fecha entre 2020 y 2100"));
  }

  @Test
  void conContraseniaIncorrectaNoCambiaLaFechaNiCierraLaSesion() throws Exception {
    Integer cambiosAntes = cambiosDeFechaAuditados();
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fecha\":\"2026-04-01T07:50:00\",\"motivo\":\"Demostración del sistema\","
                        + "\"contrasenia\":\"otra-clave\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("La contraseña no es correcta. Vuelve a escribirla."));

    assertEquals(Duration.ZERO, reloj.desfase());
    assertEquals(cambiosAntes, cambiosDeFechaAuditados());
    CuentaUsuario cuenta = cuentas.findByUsername("admin-param").orElseThrow();
    assertEquals(0, cuenta.getIntentosFallidos() == null ? 0 : cuenta.getIntentosFallidos());
    assertFalse(cuenta.estaBloqueada());
  }

  @Test
  void sinContraseniaNoCambiaLaFecha() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fecha\":\"2026-04-01T07:50:00\",\"motivo\":\"Demostración del sistema\"}"))
        .andExpect(status().isBadRequest());
    assertEquals(Duration.ZERO, reloj.desfase());
  }

  @Test
  void laContraseniaNoQuedaEnLaAuditoria() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(FECHA_DEMO))
        .andExpect(status().isOk());
    Integer conClave =
        jdbc.queryForObject(
            "select count(*) from registros_auditoria where descripcion like ?",
            Integer.class,
            "%" + CONTRASENIA + "%");
    assertEquals(0, conClave);
  }

  @Test
  void soloElAdministradorCambiaLosParametros() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(user("docente-param").roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(FECHA_DEMO))
        .andExpect(status().isForbidden());
    mvc.perform(get(RUTA).with(user("apo-param").roles("APODERADO")))
        .andExpect(status().isForbidden());
  }

  @Test
  void desactivarElPagoEnLineaBloqueaElPagoDelApoderado() throws Exception {
    mvc.perform(
            put(RUTA + "/pago-en-linea")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pagoEnLineaPermitido").value(true))
        .andExpect(jsonPath("$.pagoEnLineaActivo").value(false));

    mvc.perform(get(RUTA + "/estado").with(user("apo-param").roles("APODERADO")))
        .andExpect(jsonPath("$.pagoEnLineaDisponible").value(false));
    mvc.perform(
            post("/api/v1/apoderados/mis-pagos/pagos")
                .with(user("apo-param").roles("APODERADO"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boletaIds\":[1]}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message", startsWith("El pago en línea no está disponible")));

    mvc.perform(
            put(RUTA + "/pago-en-linea")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":false}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("No hay cambios que guardar"));
    String descripcion =
        jdbc.queryForObject(
            "select descripcion from registros_auditoria where accion='CONFIGURAR_PAGO_EN_LINEA' order by id desc limit 1",
            String.class);
    assertEquals("Pago en línea desactivado para los apoderados", descripcion);
  }

  private Integer cambiosDeFechaAuditados() {
    return jdbc.queryForObject(
        "select count(*) from registros_auditoria where accion='CAMBIAR_FECHA_SISTEMA'",
        Integer.class);
  }

  private static RequestPostProcessor admin() {
    return user("admin-param").roles("ADMINISTRADOR");
  }
}
