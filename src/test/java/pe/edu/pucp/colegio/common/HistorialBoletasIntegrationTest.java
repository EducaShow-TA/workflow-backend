package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.service.FinanzasService;

@SpringBootTest(properties = "app.jobs.catch-up-on-startup=false")
@AutoConfigureMockMvc
@Transactional
class HistorialBoletasIntegrationTest {

  @TestConfiguration
  static class Reloj {
    @Bean
    @Primary
    Clock clockHistorialBoletas() {
      return Clock.fixed(Instant.parse("2035-06-10T17:00:00Z"), ZoneId.of("America/Lima"));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired FinanzasService finanzas;
  @PersistenceContext EntityManager entityManager;

  private String sufijo;
  private String usuarioAdmin;
  private int apoderado;
  private int alumno;

  @BeforeEach
  void preparar() {
    sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    usuarioAdmin = "admin-" + sufijo.toLowerCase();
    jdbc.update(
        "insert into cuentas_usuario(username,password_hash,rol,persona_id) values (?,'hash','ADMINISTRADOR',?)",
        usuarioAdmin,
        persona("Admin"));
    apoderado = insertar("insert into apoderados(persona_id) values (?)", persona("Franco"));
    alumno = estudiante("María");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                usuarioAdmin, null, AuthorityUtils.createAuthorityList("ROLE_ADMINISTRADOR")));
  }

  @AfterEach
  void limpiarSesion() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void completar_compromisoDesdeMarzo_generaHastaElMesActualYMarcaMora() {
    int compromiso = compromiso2035(alumno);

    var resultado = finanzas.completarBoletasDeEstudiante(alumno);

    assertEquals(new FinanzasService.BoletasCompletadas(YearMonth.of(2035, 6), 4, 3), resultado);
    assertEquals(
        List.of(
            "2035-03-01 EN_MORA",
            "2035-04-01 EN_MORA",
            "2035-05-01 EN_MORA",
            "2035-06-01 PENDIENTE"),
        mesesYEstados(compromiso));
    assertEquals(4, auditoria(compromiso, "BOLETA_GENERADA", usuarioAdmin));
    assertEquals(3, auditoria(compromiso, "BOLETA_EN_MORA", "SISTEMA"));
  }

  @Test
  void completar_segundaVez_noCambiaNada() {
    int compromiso = compromiso2035(alumno);
    finanzas.completarBoletasDeEstudiante(alumno);

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 0, 0);

    assertEquals(4, mesesYEstados(compromiso).size());
  }

  @Test
  void completar_boletaPagada_noCambiaDeEstado() {
    int compromiso = compromiso2035(alumno);
    boleta(compromiso, "2035-03-01", "2035-03-15", "PAGADA");

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 3, 2);

    assertEquals("2035-03-01 PAGADA", mesesYEstados(compromiso).get(0));
  }

  @Test
  void completar_masMesesQueCuotas_respetaElCronograma() {
    int compromiso =
        compromiso(
            matricula(alumno, periodo(2035), "CONFIRMADA"), "Pensión 2035", "2035-03-01", 2, true);

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 2, 2);

    assertEquals(2, mesesYEstados(compromiso).size());
  }

  @Test
  void completar_compromisoFuturo_noGenera() {
    compromiso(
        matricula(alumno, periodo(2035), "CONFIRMADA"), "Pensión 2035", "2035-08-01", 4, true);

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 0, 0);
  }

  @Test
  void completar_dosAnios_completaAmbosCompromisos() {
    compromiso(
        matricula(alumno, periodo(2034), "CONFIRMADA"), "Pensión 2034", "2034-11-01", 2, true);
    compromiso2035(alumno);

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 6, 5);
  }

  @Test
  void completar_matriculaRetirada_noGeneraNiMarcaMora() {
    int compromiso =
        compromiso(
            matricula(alumno, periodo(2035), "RETIRADA"), "Pensión 2035", "2035-03-01", 6, true);
    boleta(compromiso, "2035-03-01", "2035-03-15", "PENDIENTE");

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 0, 0);

    assertEquals(List.of("2035-03-01 PENDIENTE"), mesesYEstados(compromiso));
  }

  @Test
  void completar_compromisoInactivo_noGenera() {
    int compromiso =
        compromiso(
            matricula(alumno, periodo(2035), "EN_REGISTRO"),
            "Pensión 2035",
            "2035-03-01",
            6,
            false);
    boleta(compromiso, "2035-03-01", "2035-03-15", "PAGADA");

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 0, 0);

    assertEquals(List.of("2035-03-01 PAGADA"), mesesYEstados(compromiso));
  }

  @Test
  void completar_otroEstudiante_noLoToca() {
    compromiso2035(alumno);
    int compromisoAjeno = compromiso2035(estudiante("Luis"));
    boleta(compromisoAjeno, "2035-03-01", "2035-03-15", "PENDIENTE");

    verificar(finanzas.completarBoletasDeEstudiante(alumno), 4, 3);

    assertEquals(List.of("2035-03-01 PENDIENTE"), mesesYEstados(compromisoAjeno));
  }

  @Test
  void historial_administrador_devuelveContratoCompleto() throws Exception {
    compromiso2035(alumno);

    mvc.perform(historial("{\"estudianteId\":" + alumno + "}").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estudianteId").value(alumno))
        .andExpect(jsonPath("$.estudiante").value("María Ruiz Pardo"))
        .andExpect(jsonPath("$.mesCorte").value("2035-06"))
        .andExpect(jsonPath("$.boletasGeneradas").value(4))
        .andExpect(jsonPath("$.boletasEnMora").value(3))
        .andExpect(jsonPath("$.totales.pagado").value(0.0))
        .andExpect(jsonPath("$.totales.pendiente").value(350.0))
        .andExpect(jsonPath("$.totales.enMora").value(1050.0))
        .andExpect(jsonPath("$.matriculas", hasSize(1)))
        .andExpect(jsonPath("$.matriculas[0].anio").value(2035))
        .andExpect(jsonPath("$.matriculas[0].estado").value("CONFIRMADA"))
        .andExpect(jsonPath("$.matriculas[0].compromiso.mesInicio").value("2035-03"))
        .andExpect(jsonPath("$.matriculas[0].boletas", hasSize(4)))
        .andExpect(jsonPath("$.matriculas[0].boletas[0].mesCobro").value("2035-03"))
        .andExpect(jsonPath("$.matriculas[0].boletas[0].estadoPago").value("EN_MORA"));
  }

  @Test
  void historial_boletaPagada_incluyeNumeroDeComprobante() throws Exception {
    int pagada = boleta(compromiso2035(alumno), "2035-03-01", "2035-03-15", "PAGADA");
    insertar(
        "insert into comprobantes_pago(boleta_id,fecha_emision,monto_pagado,medio_pago,numero_comprobante) values (?,'2035-03-10',350,'PRESENCIAL',?)",
        pagada,
        "PEN-" + sufijo);

    mvc.perform(historial("{\"estudianteId\":" + alumno + "}").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totales.pagado").value(350.0))
        .andExpect(jsonPath("$.matriculas[0].boletas[0].numeroComprobante").value("PEN-" + sufijo));
  }

  @Test
  void historial_apoderado_respondeForbidden() throws Exception {
    int compromiso = compromiso2035(alumno);

    mvc.perform(
            historial("{\"estudianteId\":" + alumno + "}")
                .with(user("apoderado-" + sufijo).roles("APODERADO")))
        .andExpect(status().isForbidden());

    assertEquals(List.of(), mesesYEstados(compromiso));
  }

  @Test
  void historial_sinSesion_respondeUnauthorized() throws Exception {
    SecurityContextHolder.clearContext();

    mvc.perform(historial("{\"estudianteId\":" + alumno + "}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void historial_sinEstudianteId_respondeBadRequest() throws Exception {
    mvc.perform(historial("{}").with(admin())).andExpect(status().isBadRequest());
  }

  @Test
  void historial_estudianteInexistente_respondeNotFound() throws Exception {
    mvc.perform(historial("{\"estudianteId\":999999}").with(admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("No existe el estudiante indicado"));
  }

  private MockHttpServletRequestBuilder historial(String cuerpo) {
    return post("/api/v1/boletas-pension/historiales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(cuerpo);
  }

  private RequestPostProcessor admin() {
    return user(usuarioAdmin).roles("ADMINISTRADOR");
  }

  private void verificar(FinanzasService.BoletasCompletadas resultado, int generadas, int enMora) {
    assertEquals(
        new FinanzasService.BoletasCompletadas(YearMonth.of(2035, 6), generadas, enMora),
        resultado);
  }

  private List<String> mesesYEstados(int compromiso) {
    entityManager.flush();
    return jdbc.query(
        "select mes_cobro, estado_pago from boletas_pension where compromiso_id=? order by mes_cobro",
        (fila, n) -> fila.getObject(1, LocalDate.class) + " " + fila.getString(2),
        compromiso);
  }

  private int auditoria(int compromiso, String accion, String usuario) {
    entityManager.flush();
    return jdbc.queryForObject(
        "select count(*) from auditoria_finanzas where compromiso_id=? and accion=? and usuario=?",
        Integer.class,
        compromiso,
        accion,
        usuario);
  }

  private int persona(String nombre) {
    return insertar(
        "insert into personas(dni,nombre,apellido_paterno,apellido_materno) values (?,?,'Ruiz','Pardo')",
        UUID.randomUUID().toString().substring(0, 8),
        nombre);
  }

  private int estudiante(String nombre) {
    int id = insertar("insert into estudiantes(persona_id) values (?)", persona(nombre));
    jdbc.update(
        "insert into vinculos_familiares(parentesco,es_principal,apoderado_id,estudiante_id) values ('PADRE',true,?,?)",
        apoderado,
        id);
    return id;
  }

  private int periodo(int anio) {
    int grado =
        jdbc.queryForObject(
            "select id from grados where numero=1 and nivel_educativo='SECUNDARIA'", Integer.class);
    int seccion =
        insertar(
            "insert into secciones(codigo,vacantes_totales,grado_id) values (?,30,?)",
            "HB" + UUID.randomUUID().toString().substring(0, 6),
            grado);
    return insertar(
        "insert into periodos(fecha_inicio,fecha_fin,estado,grado_id,seccion_id) values (?,?,'EN_CURSO',?,?)",
        LocalDate.of(anio, 3, 1),
        LocalDate.of(anio, 12, 18),
        grado,
        seccion);
  }

  private int matricula(int estudiante, int periodo, String estado) {
    return insertar(
        "insert into matriculas(fecha_matricula,estado,periodo_id,estudiante_id) values ('2034-03-01',?,?,?)",
        estado,
        periodo,
        estudiante);
  }

  private int compromiso2035(int estudiante) {
    return compromiso(
        matricula(estudiante, periodo(2035), "CONFIRMADA"), "Pensión 2035", "2035-03-01", 6, true);
  }

  private int compromiso(
      int matricula, String concepto, String mesInicio, int cuotas, boolean activo) {
    LocalDate inicio = LocalDate.parse(mesInicio);
    return insertar(
        "insert into compromisos_pago(matricula_id,apoderado_id,concepto,monto_total,fecha_emision,mes_inicio,numero_cuotas,primer_vencimiento,dia_vencimiento,active) values (?,?,?,?,?,?,?,?,15,?)",
        matricula,
        apoderado,
        concepto,
        350 * cuotas,
        inicio,
        inicio,
        cuotas,
        inicio.withDayOfMonth(15),
        activo);
  }

  private int boleta(int compromiso, String mes, String vencimiento, String estado) {
    return insertar(
        "insert into boletas_pension(compromiso_id,mes_cobro,fecha_vencimiento,monto,mora,estado_pago,mecanismo_generacion,fecha_generacion,active) values (?,?,?,350,0,?,'AUTOMATICO',CURRENT_TIMESTAMP,true)",
        compromiso,
        mes,
        vencimiento,
        estado);
  }

  private int insertar(String sql, Object... valores) {
    GeneratedKeyHolder llave = new GeneratedKeyHolder();
    jdbc.update(
        conexion -> {
          var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < valores.length; i++) {
            sentencia.setObject(i + 1, valores[i]);
          }
          return sentencia;
        },
        llave);
    return llave.getKey().intValue();
  }
}
