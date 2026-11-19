package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.CompromisoPagoRequest;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.repository.BoletaPensionRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.service.AuditoriaFinancieraService;
import pe.edu.pucp.colegio.common.service.FinanzasService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "caja-auditoria", roles = "ADMINISTRADOR")
class AuditoriaFinancieraIntegrationTest {

  private static final String RUTA = "/api/v1/auditoria-finanzas";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired FinanzasService finanzas;
  @Autowired AuditoriaFinancieraService auditoria;
  @Autowired MatriculaRepository matriculas;
  @Autowired BoletaPensionRepository boletas;
  @Autowired Clock clock;

  private String dniEstudiante;
  private int gradoId;
  private int seccionId;
  private int matriculaId;
  private int boletaId;
  private BigDecimal cuota;

  @BeforeEach
  void prepararCompromisoYPago() throws Exception {
    String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    gradoId =
        jdbc.queryForObject(
            "select id from grados where numero=1 and nivel_educativo='SECUNDARIA'", Integer.class);
    seccionId =
        insertar(
            "insert into secciones(codigo,vacantes_totales,grado_id) values (?,30,?)",
            "AF" + sufijo,
            gradoId);
    int periodo =
        insertar(
            "insert into periodos(fecha_inicio,fecha_fin,estado,grado_id,seccion_id) values ('2035-01-01','2035-12-31','EN_CURSO',?,?)",
            gradoId,
            seccionId);
    dniEstudiante = "E" + sufijo;
    int estudiante =
        insertar("insert into estudiantes(persona_id) values (?)", persona(dniEstudiante, "Ana"));
    int apoderado =
        insertar("insert into apoderados(persona_id) values (?)", persona("A" + sufijo, "Rosa"));
    jdbc.update(
        "insert into vinculos_familiares(parentesco,es_principal,apoderado_id,estudiante_id) values ('MADRE',true,?,?)",
        apoderado,
        estudiante);
    matriculaId =
        insertar(
            "insert into matriculas(fecha_matricula,estado,periodo_id,estudiante_id) values ('2035-01-01','CONFIRMADA',?,?)",
            periodo,
            estudiante);

    LocalDate vencimiento = LocalDate.now(clock).plusMonths(2).withDayOfMonth(15);
    finanzas.crearCompromiso(
        matriculas.findById(matriculaId).orElseThrow(),
        new CompromisoPagoRequest("Pensión", new BigDecimal("700.00"), 2, vencimiento, 15));
    BoletaPension boleta =
        boletas.findAll().stream()
            .filter(b -> b.getCompromiso().getMatricula().getId().equals(matriculaId))
            .findFirst()
            .orElseThrow();
    boletaId = boleta.getId();
    cuota = boleta.calcularMontoTotal();

    mvc.perform(
            post("/api/v1/boletas-pension/{id}/pagos", boletaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"monto\":"
                        + cuota
                        + ",\"medioPago\":\"PRESENCIAL\",\"referencia\":\"AUD-"
                        + sufijo
                        + "\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void registraCompromisoBoletaYPagoConQuienLoHizoYElApoderadoTitular() throws Exception {
    mvc.perform(get(RUTA).param("estudiante", dniEstudiante))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(3))
        .andExpect(jsonPath("$.content[0].accion").value("PAGO_REGISTRADO"))
        .andExpect(jsonPath("$.content[0].usuario").value("caja-auditoria"))
        .andExpect(jsonPath("$.content[0].rolUsuario").value("ADMINISTRADOR"))
        .andExpect(jsonPath("$.content[0].apoderadoNombre").value(containsString("Rosa")))
        .andExpect(jsonPath("$.content[0].estudianteNombre").value(containsString("Ana")))
        .andExpect(jsonPath("$.content[0].estadoAnterior").value("PENDIENTE"))
        .andExpect(jsonPath("$.content[0].estadoNuevo").value("PAGADA"))
        .andExpect(jsonPath("$.content[0].medioPago").value("PRESENCIAL"))
        .andExpect(jsonPath("$.content[0].numeroComprobante").value(containsString("AUD-")))
        .andExpect(jsonPath("$.content[0].monto").value(cuota.doubleValue()))
        .andExpect(jsonPath("$.content[0].boletaId").value(boletaId))
        .andExpect(jsonPath("$.content[1].accion").value("BOLETA_GENERADA"))
        .andExpect(jsonPath("$.content[1].estadoNuevo").value("PENDIENTE"))
        .andExpect(jsonPath("$.content[1].mecanismo").value("AUTOMATICO"))
        .andExpect(jsonPath("$.content[2].accion").value("COMPROMISO_GENERADO"))
        .andExpect(jsonPath("$.content[2].numeroCuotas").value(2))
        .andExpect(jsonPath("$.content[2].monto").value(700.0))
        .andExpect(jsonPath("$.content[2].estadoAnterior").value(nullValue()));
  }

  @Test
  void resumenYFiltrosSeAplicanSobreTodosLosEventos() throws Exception {
    mvc.perform(get(RUTA + "/resumen").param("estudiante", dniEstudiante))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventos").value(3))
        .andExpect(jsonPath("$.pagos").value(1))
        .andExpect(jsonPath("$.montoPagado").value(cuota.doubleValue()))
        .andExpect(jsonPath("$.boletasGeneradas").value(1))
        .andExpect(jsonPath("$.compromisosGenerados").value(1))
        .andExpect(jsonPath("$.boletasEnMora").value(0));

    String hoy = LocalDate.now(clock).toString();
    List<String[]> filtrosConUnEvento =
        List.of(
            new String[] {"accion", "PAGO_REGISTRADO"},
            new String[] {"medioPago", "PRESENCIAL"},
            new String[] {"comprobante", "aud-"});
    for (String[] filtro : filtrosConUnEvento) {
      mvc.perform(get(RUTA).param("estudiante", dniEstudiante).param(filtro[0], filtro[1]))
          .andExpect(jsonPath("$.page.totalElements").value(1));
    }
    mvc.perform(get(RUTA).param("boletaId", "" + boletaId))
        .andExpect(jsonPath("$.page.totalElements").value(2));
    mvc.perform(
            get(RUTA)
                .param("gradoId", "" + gradoId)
                .param("seccionId", "" + seccionId)
                .param("desde", hoy)
                .param("hasta", hoy))
        .andExpect(jsonPath("$.page.totalElements").value(3));
    mvc.perform(get(RUTA).param("estudiante", "ana prueba"))
        .andExpect(jsonPath("$.content[0].estudianteNombre").value(containsString("Ana")));
    mvc.perform(
            get(RUTA)
                .param("estudiante", dniEstudiante)
                .param("desde", LocalDate.now(clock).plusDays(1).toString()))
        .andExpect(jsonPath("$.page.totalElements").value(0));
    mvc.perform(get(RUTA).param("estudiante", dniEstudiante).param("medioPago", "VIRTUAL"))
        .andExpect(jsonPath("$.page.totalElements").value(0));
    mvc.perform(get(RUTA).param("comprobante", "100%"))
        .andExpect(jsonPath("$.page.totalElements").value(0));
  }

  @Test
  void laMoraYLoAutomaticoQuedanARegistradosPorElSistema() throws Exception {
    BoletaPension boleta = boletas.findById(boletaId).orElseThrow();
    boleta.setEstadoPago(EstadoBoleta.PENDIENTE);
    boleta.marcarEnMora(boleta.getFechaVencimiento().plusDays(1));
    auditoria.registrarMora(boleta, EstadoBoleta.PENDIENTE);
    SecurityContextHolder.clearContext();
    auditoria.registrarBoletaGenerada(boleta);

    mvc.perform(
            get(RUTA)
                .with(user("caja-auditoria").roles("ADMINISTRADOR"))
                .param("boletaId", "" + boletaId))
        .andExpect(jsonPath("$.page.totalElements").value(4))
        .andExpect(jsonPath("$.content[0].usuario").value("SISTEMA"))
        .andExpect(jsonPath("$.content[1].accion").value("BOLETA_EN_MORA"))
        .andExpect(jsonPath("$.content[1].usuario").value("SISTEMA"))
        .andExpect(jsonPath("$.content[1].estadoAnterior").value("PENDIENTE"))
        .andExpect(jsonPath("$.content[1].estadoNuevo").value("EN_MORA"));
  }

  @Test
  void exportaTodoLoFiltradoEnCsvConEtiquetasLegibles() throws Exception {
    String csv =
        mvc.perform(get(RUTA + "/csv").param("estudiante", dniEstudiante))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString("auditoria-finanzas")))
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    String[] lineas = csv.split("\r\n");
    assertEquals(4, lineas.length);
    assertTrue(lineas[0].contains("Fecha y hora,Acción,Estudiante"));
    assertTrue(lineas[1].contains("Pago registrado"));
    assertTrue(lineas[1].contains("Pendiente,Pagada"));
    assertTrue(lineas[3].contains("Compromiso generado"));
  }

  @Test
  void validaParametrosYRestringeAlAdministrador() throws Exception {
    mvc.perform(get(RUTA).param("desde", "2026-11-20").param("hasta", "2026-11-01"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(RUTA).param("size", "101")).andExpect(status().isBadRequest());

    for (String ruta : List.of(RUTA, RUTA + "/resumen", RUTA + "/csv")) {
      mvc.perform(get(ruta).with(user("docente-auditoria").roles("DOCENTE")))
          .andExpect(status().isForbidden());
    }
  }

  private int persona(String dni, String nombre) {
    return insertar(
        "insert into personas(dni,nombre,apellido_paterno,apellido_materno) values (?,?,'Prueba','Finanzas')",
        dni,
        nombre);
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
