package pe.edu.pucp.colegio.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Consulta real de repositorios y agregados en H2 aislada; no usa datos de desarrollo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMINISTRADOR")
class FinanzasPaginacionIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  int estudiante1;
  int grado1;
  int seccion1;
  int periodo1;

  @BeforeEach
  void preparar() {
    grado1 =
        jdbc.queryForObject(
            "select id from grados where numero=1 and nivel_educativo='SECUNDARIA'", Integer.class);
    int grado2 =
        jdbc.queryForObject(
            "select id from grados where numero=2 and nivel_educativo='SECUNDARIA'", Integer.class);
    seccion1 =
        insertar(
            "insert into secciones(codigo,vacantes_totales,grado_id) values ('XP',30,?)", grado1);
    int seccion2 =
        insertar(
            "insert into secciones(codigo,vacantes_totales,grado_id) values ('XQ',30,?)", grado2);
    periodo1 = periodo(grado1, seccion1);
    int periodo2 = periodo(grado2, seccion2);
    int apoderado1 =
        insertar("insert into apoderados(persona_id) values (?)", persona("PAGAP01", "Rosa"));
    int apoderado2 =
        insertar("insert into apoderados(persona_id) values (?)", persona("PAGAP02", "Jose"));
    estudiante1 =
        insertar("insert into estudiantes(persona_id) values (?)", persona("PAGAL01", "Ana"));
    int estudiante2 =
        insertar("insert into estudiantes(persona_id) values (?)", persona("PAGAL02", "Luis"));
    int compromiso1 = compromiso(estudiante1, periodo1, apoderado1);
    int compromiso2 = compromiso(estudiante2, periodo2, apoderado2);
    boleta(compromiso1, "2035-01-01", "2035-03-15", 100, 10, "EN_MORA", true);
    boleta(compromiso1, "2035-02-01", "2035-03-15", 100, 5, "EN_MORA", true);
    boleta(compromiso1, "2035-03-01", "2035-04-15", 100, 0, "PENDIENTE", true);
    boleta(compromiso1, "2035-04-01", "2035-02-15", 100, 0, "PAGADA", true);
    boleta(compromiso2, "2035-01-01", "2035-03-20", 200, 0, "EN_MORA", true);
    boleta(compromiso2, "2035-02-01", "2035-04-20", 200, 0, "PENDIENTE", true);
    boleta(compromiso2, "2035-03-01", "2035-03-10", 999, 0, "EN_MORA", false);
  }

  @Test
  void paginaYAgregadosNoDependenDelTamanoNiDeLaPagina() throws Exception {
    for (int pagina = 0; pagina < 3; pagina++) {
      mvc.perform(
              get("/api/v1/reportes/boletas-pension/pagina")
                  .param("page", "" + pagina)
                  .param("size", "2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.pagina.totalElements").value(5))
          .andExpect(jsonPath("$.pagina.totalPages").value(3))
          .andExpect(jsonPath("$.pagina.number").value(pagina))
          .andExpect(jsonPath("$.pagina.content.length()").value(pagina == 2 ? 1 : 2))
          .andExpect(jsonPath("$.montoPendiente").value(715))
          .andExpect(jsonPath("$.familiasMorosas").value(2));
    }
  }

  @Test
  void filtrosCombinadosAplicanALaPaginaYAlResumenIncluyendoLimites() throws Exception {
    mvc.perform(
            get("/api/v1/reportes/boletas-pension/pagina")
                .param("gradoId", "" + grado1)
                .param("seccionId", "" + seccion1)
                .param("estado", "EN_MORA")
                .param("vencimientoDesde", "2035-03-15")
                .param("vencimientoHasta", "2035-03-15")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pagina.totalElements").value(2))
        .andExpect(jsonPath("$.pagina.content.length()").value(1))
        .andExpect(jsonPath("$.montoPendiente").value(215))
        .andExpect(jsonPath("$.familiasMorosas").value(1));
    mvc.perform(
            get("/api/v1/boletas-pension/pagina")
                .param("estudianteId", "" + estudiante1)
                .param("periodoId", "" + periodo1)
                .param("mes", "2035-03")
                .param("estado", "PENDIENTE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].montoTotal").value(100));
  }

  @Test
  void ordenEsEstableYFinanzasIncluyePagadas() throws Exception {
    var primera =
        json.readTree(
            mvc.perform(get("/api/v1/reportes/boletas-pension/pagina").param("size", "1"))
                .andReturn()
                .getResponse()
                .getContentAsString());
    var segunda =
        json.readTree(
            mvc.perform(
                    get("/api/v1/reportes/boletas-pension/pagina")
                        .param("page", "1")
                        .param("size", "1"))
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(primera.at("/pagina/content/0/id").asInt())
        .isLessThan(segunda.at("/pagina/content/0/id").asInt());
    mvc.perform(get("/api/v1/boletas-pension/pagina").param("estado", "PAGADA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].estadoPago").value("PAGADA"));
  }

  @Test
  void vaciosYFueraDeRangoConservanLosTotalesReales() throws Exception {
    mvc.perform(get("/api/v1/reportes/boletas-pension/pagina").param("estado", "PENDIENTE"))
        .andExpect(jsonPath("$.montoPendiente").value(300))
        .andExpect(jsonPath("$.familiasMorosas").value(0));
    mvc.perform(get("/api/v1/reportes/boletas-pension/pagina").param("page", "99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pagina.content").isEmpty())
        .andExpect(jsonPath("$.pagina.totalElements").value(5))
        .andExpect(jsonPath("$.montoPendiente").value(715));
    mvc.perform(get("/api/v1/reportes/boletas-pension/pagina").param("gradoId", "999999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pagina.totalElements").value(0))
        .andExpect(jsonPath("$.montoPendiente").value(0))
        .andExpect(jsonPath("$.familiasMorosas").value(0));
  }

  @Test
  void conservaContratosAnterioresYElCatalogoSoloDevuelveEstudiantes() throws Exception {
    mvc.perform(get("/api/v1/boletas-pension").param("estado", "PAGADA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/v1/reportes/boletas-pension"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.boletas.length()").value(6))
        .andExpect(jsonPath("$.montoPendiente").value(715));
    mvc.perform(get("/api/v1/boletas-pension/estudiantes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].nombre").exists())
        .andExpect(jsonPath("$[0].monto").doesNotExist());
  }

  @Test
  void validaPaginacionFechasYEstados() throws Exception {
    for (String[] parametros :
        List.of(
            new String[] {"page", "-1"},
            new String[] {"size", "0"},
            new String[] {"size", "101"})) {
      mvc.perform(get("/api/v1/boletas-pension/pagina").param(parametros[0], parametros[1]))
          .andExpect(status().isBadRequest());
      mvc.perform(
              get("/api/v1/reportes/boletas-pension/pagina").param(parametros[0], parametros[1]))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(get("/api/v1/reportes/boletas-pension/pagina").param("estado", "PAGADA"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(
            get("/api/v1/reportes/boletas-pension/pagina")
                .param("vencimientoDesde", "2035-04-01")
                .param("vencimientoHasta", "2035-03-01"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithMockUser(roles = "DOCENTE")
  void noExponeBoletasNiAgregadosAOtrosRoles() throws Exception {
    for (String ruta :
        List.of(
            "/boletas-pension/pagina",
            "/boletas-pension/estudiantes",
            "/reportes/boletas-pension/pagina")) {
      mvc.perform(get("/api/v1" + ruta)).andExpect(status().isForbidden());
    }
  }

  private int persona(String dni, String nombre) {
    return insertar(
        "insert into personas(dni,nombre,apellido_paterno,apellido_materno) values (?,?,'Prueba','SQL')",
        dni,
        nombre);
  }

  private int periodo(int grado, int seccion) {
    return insertar(
        "insert into periodos(fecha_inicio,fecha_fin,estado,grado_id,seccion_id) values ('2035-01-01','2035-12-31','EN_CURSO',?,?)",
        grado,
        seccion);
  }

  private int compromiso(int estudiante, int periodo, int apoderado) {
    int matricula =
        insertar(
            "insert into matriculas(fecha_matricula,estado,periodo_id,estudiante_id) values ('2035-01-01','CONFIRMADA',?,?)",
            periodo,
            estudiante);
    return insertar(
        "insert into compromisos_pago(matricula_id,apoderado_id,concepto,monto_total,fecha_emision,mes_inicio,numero_cuotas,primer_vencimiento,dia_vencimiento) values (?,?,'Pension',1000,'2035-01-01','2035-01-01',4,'2035-01-15',15)",
        matricula,
        apoderado);
  }

  private void boleta(
      int compromiso,
      String mes,
      String vencimiento,
      int monto,
      int mora,
      String estado,
      boolean activa) {
    insertar(
        "insert into boletas_pension(compromiso_id,mes_cobro,fecha_vencimiento,monto,mora,estado_pago,mecanismo_generacion,fecha_generacion,active) values (?,?,?,?,?,?,'MANUAL',CURRENT_TIMESTAMP,?)",
        compromiso,
        mes,
        vencimiento,
        monto,
        mora,
        estado,
        activa);
  }

  private int insertar(String sql, Object... parametros) {
    var clave = new GeneratedKeyHolder();
    jdbc.update(
        conexion -> {
          var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < parametros.length; i++) sentencia.setObject(i + 1, parametros[i]);
          return sentencia;
        },
        clave);
    return clave.getKey().intValue();
  }
}
