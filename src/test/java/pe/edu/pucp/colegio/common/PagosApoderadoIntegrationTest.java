package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.pagos.simulado.enabled=true")
@AutoConfigureMockMvc
@Transactional
class PagosApoderadoIntegrationTest {

  private static final String RUTA = "/api/v1/apoderados/mis-pagos";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @PersistenceContext EntityManager entityManager;

  private String sufijo;
  private String usuarioApoderado;
  private int boletaPagada;
  private int boletaEnMora;
  private int boletaPendiente;
  private int boletaAjena;
  private int comprobantePagado;

  @BeforeEach
  void prepararFamilia() {
    sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    int grado =
        jdbc.queryForObject(
            "select id from grados where numero=1 and nivel_educativo='SECUNDARIA'", Integer.class);
    int seccion =
        insertar(
            "insert into secciones(codigo,vacantes_totales,grado_id) values (?,30,?)",
            "PG" + sufijo,
            grado);
    int periodo =
        insertar(
            "insert into periodos(fecha_inicio,fecha_fin,estado,grado_id,seccion_id) values ('2035-03-01','2035-12-18','EN_CURSO',?,?)",
            grado,
            seccion);

    int personaApoderado = persona("A" + sufijo, "Franco");
    int apoderado = insertar("insert into apoderados(persona_id) values (?)", personaApoderado);
    usuarioApoderado = "apo-" + sufijo.toLowerCase();
    jdbc.update(
        "insert into cuentas_usuario(username,password_hash,rol,persona_id) values (?,'hash','APODERADO',?)",
        usuarioApoderado,
        personaApoderado);

    int hija =
        insertar("insert into estudiantes(persona_id) values (?)", persona("H" + sufijo, "María"));
    vincular(apoderado, hija);
    int compromiso = compromiso(hija, periodo, apoderado, 4);
    boletaPagada = boleta(compromiso, "2035-03-01", "2035-03-15", "PAGADA");
    comprobantePagado =
        insertar(
            "insert into comprobantes_pago(boleta_id,fecha_emision,monto_pagado,medio_pago,numero_comprobante) values (?,'2035-03-10',350,'PRESENCIAL',?)",
            boletaPagada,
            "PEN-" + sufijo);
    boletaEnMora = boleta(compromiso, "2035-04-01", "2035-04-15", "EN_MORA");
    boletaPendiente = boleta(compromiso, "2035-05-01", "2035-05-15", "PENDIENTE");

    int otroApoderado =
        insertar("insert into apoderados(persona_id) values (?)", persona("B" + sufijo, "Rosa"));
    int ajeno =
        insertar("insert into estudiantes(persona_id) values (?)", persona("J" + sufijo, "Luis"));
    vincular(otroApoderado, ajeno);
    boletaAjena =
        boleta(
            compromiso(ajeno, periodo, otroApoderado, 2), "2035-03-01", "2035-03-15", "PENDIENTE");
  }

  @Test
  void muestraSoloLosHijosVinculadosConSuCompromisoYBoletasEmitidas() throws Exception {
    mvc.perform(get(RUTA).with(apoderado()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].nombreCompleto").value(containsString("María")))
        .andExpect(jsonPath("$[0].compromisos", hasSize(1)))
        .andExpect(jsonPath("$[0].compromisos[0].anio").value(2035))
        .andExpect(jsonPath("$[0].compromisos[0].apoderadoTitular").value(containsString("Franco")))
        .andExpect(jsonPath("$[0].compromisos[0].montoTotal").value(1400.0))
        .andExpect(jsonPath("$[0].compromisos[0].montoPagado").value(350.0))
        .andExpect(jsonPath("$[0].compromisos[0].montoPorPagar").value(700.0))
        .andExpect(jsonPath("$[0].compromisos[0].cuotasPagadas").value(1))
        .andExpect(jsonPath("$[0].compromisos[0].cuotasEnMora").value(1))
        .andExpect(jsonPath("$[0].compromisos[0].cuotasPendientes").value(1))
        .andExpect(jsonPath("$[0].compromisos[0].cuotasPorEmitir").value(1))
        .andExpect(jsonPath("$[0].compromisos[0].proximaEmision").value("2035-06-01"))
        .andExpect(jsonPath("$[0].compromisos[0].situacion").value("CON_MORA"))
        .andExpect(jsonPath("$[0].compromisos[0].boletas", hasSize(3)))
        .andExpect(
            jsonPath("$[0].compromisos[0].boletas[0].comprobante.numeroComprobante")
                .value("PEN-" + sufijo))
        .andExpect(jsonPath("$[0].compromisos[0].boletas[1].estadoPago").value("EN_MORA"))
        .andExpect(jsonPath("$[0].compromisos[0].boletas[1].comprobante").value(nullValue()));
  }

  @Test
  void pagaVariasBoletasJuntasComoVirtualYQuedaEnLaAuditoria() throws Exception {
    String respuesta =
        mvc.perform(pagar(boletaPendiente, boletaEnMora))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].boletaId").value(boletaEnMora))
            .andExpect(jsonPath("$[0].medioPago").value("VIRTUAL"))
            .andExpect(jsonPath("$[0].numeroComprobante").value(startsWith("SIM-")))
            .andExpect(jsonPath("$[1].boletaId").value(boletaPendiente))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode comprobantes = json.readTree(respuesta);
    assertTrue(
        !comprobantes
            .get(0)
            .get("numeroComprobante")
            .asText()
            .equals(comprobantes.get(1).get("numeroComprobante").asText()));

    entityManager.flush();
    assertEquals(
        2,
        jdbc.queryForObject(
            "select count(*) from boletas_pension where id in (?,?) and estado_pago='PAGADA'",
            Integer.class,
            boletaEnMora,
            boletaPendiente));
    assertEquals(
        2,
        jdbc.queryForObject(
            "select count(*) from auditoria_finanzas where accion='PAGO_REGISTRADO' and usuario=? and rol_usuario='APODERADO' and medio_pago='VIRTUAL'",
            Integer.class,
            usuarioApoderado));

    mvc.perform(get(RUTA).with(apoderado()))
        .andExpect(jsonPath("$[0].compromisos[0].situacion").value("AL_DIA"))
        .andExpect(jsonPath("$[0].compromisos[0].montoPorPagar").value(0.0));
  }

  @Test
  void siUnaBoletaNoSePuedePagarNoSeCobraNinguna() throws Exception {
    mvc.perform(pagar(boletaPendiente, boletaPagada))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(containsString("ya tiene un pago registrado")));
    mvc.perform(pagar(boletaPendiente, boletaAjena)).andExpect(status().isForbidden());
    mvc.perform(pagar(boletaPendiente, boletaPendiente)).andExpect(status().isBadRequest());
    mvc.perform(pagar(boletaPendiente, 999_999)).andExpect(status().isNotFound());
    mvc.perform(
            post(RUTA + "/pagos")
                .with(apoderado())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"boletaIds\":[]}"))
        .andExpect(status().isBadRequest());

    assertEquals(
        "PENDIENTE",
        jdbc.queryForObject(
            "select estado_pago from boletas_pension where id=?", String.class, boletaPendiente));
    assertEquals(
        "PENDIENTE",
        jdbc.queryForObject(
            "select estado_pago from boletas_pension where id=?", String.class, boletaAjena));
  }

  @Test
  void descargaElComprobanteSoloDeSusHijos() throws Exception {
    byte[] archivo =
        mvc.perform(get(RUTA + "/comprobantes/{id}/pdf", comprobantePagado).with(apoderado()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", containsString("PEN-" + sufijo)))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String texto = new PdfTextExtractor(new PdfReader(archivo)).getTextFromPage(1);
    assertTrue(texto.contains("PEN-" + sufijo));
    assertTrue(texto.contains("María"));
    assertTrue(texto.contains("No es un comprobante electrónico"));

    int comprobanteAjeno =
        insertar(
            "insert into comprobantes_pago(boleta_id,fecha_emision,monto_pagado,medio_pago,numero_comprobante) values (?,'2035-03-10',350,'PRESENCIAL',?)",
            boletaAjena,
            "PEN-X" + sufijo);
    mvc.perform(get(RUTA + "/comprobantes/{id}/pdf", comprobanteAjeno).with(apoderado()))
        .andExpect(status().isForbidden());
    mvc.perform(get(RUTA + "/comprobantes/{id}/pdf", 999_999).with(apoderado()))
        .andExpect(status().isNotFound());
  }

  @Test
  void soloElApoderadoConCuentaActivaPuedeUsarlo() throws Exception {
    mvc.perform(get(RUTA).with(user("admin-pagos").roles("ADMINISTRADOR")))
        .andExpect(status().isForbidden());
    mvc.perform(get(RUTA).with(user("sin-cuenta-" + sufijo).roles("APODERADO")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("La cuenta no corresponde a un apoderado activo"));
    jdbc.update("update cuentas_usuario set active=false where username=?", usuarioApoderado);
    mvc.perform(get(RUTA).with(apoderado())).andExpect(status().isForbidden());
  }

  private RequestPostProcessor apoderado() {
    return user(usuarioApoderado).roles("APODERADO");
  }

  private org.springframework.test.web.servlet.RequestBuilder pagar(int... ids) {
    StringBuilder lista = new StringBuilder();
    for (int id : ids) {
      lista.append(lista.isEmpty() ? "" : ",").append(id);
    }
    return post(RUTA + "/pagos")
        .with(apoderado())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"boletaIds\":[" + lista + "]}");
  }

  private int persona(String dni, String nombre) {
    return insertar(
        "insert into personas(dni,nombre,apellido_paterno,apellido_materno) values (?,?,'Ruiz','Pardo')",
        dni,
        nombre);
  }

  private void vincular(int apoderado, int estudiante) {
    jdbc.update(
        "insert into vinculos_familiares(parentesco,es_principal,apoderado_id,estudiante_id) values ('PADRE',true,?,?)",
        apoderado,
        estudiante);
  }

  private int compromiso(int estudiante, int periodo, int apoderado, int cuotas) {
    int matricula =
        insertar(
            "insert into matriculas(fecha_matricula,estado,periodo_id,estudiante_id) values ('2035-03-01','CONFIRMADA',?,?)",
            periodo,
            estudiante);
    return insertar(
        "insert into compromisos_pago(matricula_id,apoderado_id,concepto,monto_total,fecha_emision,mes_inicio,numero_cuotas,primer_vencimiento,dia_vencimiento) values (?,?,'Pensión 2035',?,'2035-03-01','2035-03-01',?,'2035-03-15',15)",
        matricula,
        apoderado,
        350 * cuotas,
        cuotas);
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
