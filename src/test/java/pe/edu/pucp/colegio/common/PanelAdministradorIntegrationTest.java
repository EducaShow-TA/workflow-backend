package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PanelAdministradorIntegrationTest {

  private static final String RUTA = "/api/v1/panel/administrador";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;

  @Test
  @WithMockUser(username = "docente-panel", roles = "DOCENTE")
  void soloElAdministradorConsultaElPanel() throws Exception {
    mvc.perform(get(RUTA)).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin-panel", roles = "ADMINISTRADOR")
  void reflejaMatriculasCobranzaErroresYActividad() throws Exception {
    LocalDate hoy = LocalDate.now(clock);
    String antes = consultar();

    String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    int gradoId =
        jdbc.queryForObject(
            "select id from grados where numero=1 and nivel_educativo='SECUNDARIA'", Integer.class);
    int seccionId =
        insertar(
            "insert into secciones(codigo,vacantes_totales,grado_id) values (?,30,?)",
            "PN" + sufijo,
            gradoId);
    int periodo =
        insertar(
            "insert into periodos(fecha_inicio,fecha_fin,estado,grado_id,seccion_id) values (?,?,'EN_CURSO',?,?)",
            LocalDate.of(hoy.getYear(), 1, 1),
            LocalDate.of(hoy.getYear(), 12, 31),
            gradoId,
            seccionId);
    int confirmado = estudiante("C" + sufijo);
    int enRegistro = estudiante("R" + sufijo);
    int matricula = matricula(periodo, confirmado, "CONFIRMADA");
    int matriculaEnRegistro = matricula(periodo, enRegistro, "EN_REGISTRO");

    int apoderado =
        insertar("insert into apoderados(persona_id) values (?)", persona("A" + sufijo, "Rosa"));
    int compromiso = compromiso(matricula, apoderado, hoy);
    boleta(compromiso, hoy, "300.00", "PAGADA");
    boleta(compromiso(matriculaEnRegistro, apoderado, hoy), hoy, "200.00", "EN_MORA");

    jdbc.update(
        "insert into registros_error(fecha_hora,nivel,mensaje) values (?,'CRITICO','Falla de prueba')",
        LocalDateTime.now(clock));
    jdbc.update(
        "insert into registros_error(fecha_hora,nivel,mensaje) values (?,'ERROR','Falla antigua')",
        LocalDateTime.now(clock).minusDays(3));

    int docente =
        insertar("insert into docentes(persona_id) values (?)", persona("D" + sufijo, "Luis"));
    jdbc.update(
        "insert into asistencias_docente(docente_id,fecha,hora_entrada,estado) values (?,?,'07:40','PRESENTE')",
        docente,
        hoy);

    jdbc.update(
        "insert into auditoria_finanzas(fecha_hora,usuario,rol_usuario,accion,compromiso_id,estudiante_id,estudiante_nombre,estudiante_dni,grado_id,grado_numero,seccion_id,seccion_codigo,apoderado_id,apoderado_nombre,estado_nuevo,mes_cobro,monto) values (?,'caja','ADMINISTRADOR','PAGO_REGISTRADO',?,?,'Ana Panel',?,?,1,?,?,?,'Rosa','PAGADA',?,300)",
        LocalDateTime.now(clock).plusDays(1),
        compromiso,
        confirmado,
        "C" + sufijo,
        gradoId,
        seccionId,
        "PN" + sufijo,
        apoderado,
        hoy.withDayOfMonth(1));

    String despues = consultar();
    int mes = hoy.getMonthValue();

    assertEquals(1, diferencia(antes, despues, "$.pendientes.matriculasPorConfirmar"));
    assertEquals(1, diferencia(antes, despues, "$.pendientes.familiasEnMora"));
    assertEquals(200, diferencia(antes, despues, "$.pendientes.montoEnMora"));
    assertEquals(1, diferencia(antes, despues, "$.pendientes.erroresUltimas24h"));
    assertEquals(1, diferencia(antes, despues, "$.pendientes.erroresCriticos24h"));
    assertEquals(2, diferencia(antes, despues, "$.indicadores.estudiantesMatriculados"));
    assertEquals(1, diferencia(antes, despues, "$.indicadores.estudiantesConfirmados"));
    assertTrue(
        JsonPath.<List<Integer>>read(despues, "$.pendientes.anhiosPorConfirmar")
            .contains(hoy.getYear()));
    assertEquals(30, diferencia(antes, despues, "$.indicadores.vacantesTotales"));
    assertEquals(500, diferencia(antes, despues, "$.indicadores.pensionMesEmitida"));
    assertEquals(300, diferencia(antes, despues, "$.indicadores.pensionMesCobrada"));
    assertEquals(1, diferencia(antes, despues, "$.indicadores.docentesPresentesHoy"));
    String grado = "$.ocupacion[?(@.nivelEducativo=='SECUNDARIA' && @.grado==1)].";
    assertEquals(1, diferencia(antes, despues, grado + "secciones"));
    assertEquals(30, diferencia(antes, despues, grado + "vacantes"));
    assertEquals(1, diferencia(antes, despues, grado + "pendientes"));
    String cobranza = "$.cobranza[?(@.mes==" + mes + ")].";
    assertEquals(300, diferencia(antes, despues, cobranza + "pagado"));
    assertEquals(200, diferencia(antes, despues, cobranza + "vencido"));
    assertEquals("PAGO", JsonPath.read(despues, "$.actividad[0].tipo"));
    String detalle = JsonPath.read(despues, "$.actividad[0].detalle");
    assertTrue(detalle.contains("Ana Panel") && detalle.contains("S/ 300.00"), detalle);
  }

  @Test
  @WithMockUser(username = "admin-panel", roles = "ADMINISTRADOR")
  void informaLaVentanaDeMatriculaVigente() throws Exception {
    LocalDate hoy = LocalDate.now(clock);
    jdbc.update("update ventanas_matricula set active=false");
    jdbc.update(
        "insert into ventanas_matricula(fecha_inicio,fecha_limite) values (?,?)",
        hoy.minusDays(2),
        hoy.plusDays(10));

    mvc.perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fecha").value(hoy.toString()))
        .andExpect(jsonPath("$.ventanaMatricula.estado").value("VIGENTE"))
        .andExpect(jsonPath("$.ventanaMatricula.fechaLimite").value(hoy.plusDays(10).toString()));
  }

  private String consultar() throws Exception {
    return mvc.perform(get(RUTA))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static double diferencia(String antes, String despues, String ruta) {
    return valor(despues, ruta) - valor(antes, ruta);
  }

  private static double valor(String json, String ruta) {
    Object leido = JsonPath.read(json, ruta);
    if (leido instanceof List<?> lista) {
      return lista.isEmpty() ? 0 : ((Number) lista.get(0)).doubleValue();
    }
    return ((Number) leido).doubleValue();
  }

  private int compromiso(int matricula, int apoderado, LocalDate hoy) {
    return insertar(
        "insert into compromisos_pago(matricula_id,apoderado_id,concepto,monto_total,fecha_emision,mes_inicio,numero_cuotas,primer_vencimiento,dia_vencimiento) values (?,?,'Pensión',500,?,?,2,?,15)",
        matricula,
        apoderado,
        hoy,
        hoy.withDayOfMonth(1),
        hoy.withDayOfMonth(15));
  }

  private void boleta(int compromiso, LocalDate hoy, String monto, String estado) {
    jdbc.update(
        "insert into boletas_pension(compromiso_id,mes_cobro,monto,fecha_vencimiento,estado_pago,mecanismo_generacion,fecha_generacion) values (?,?,?,?,?,'AUTOMATICO',?)",
        compromiso,
        hoy.withDayOfMonth(1),
        new BigDecimal(monto),
        hoy.withDayOfMonth(1),
        estado,
        LocalDateTime.now(clock));
  }

  private int estudiante(String dni) {
    return insertar("insert into estudiantes(persona_id) values (?)", persona(dni, "Alumno"));
  }

  private int matricula(int periodo, int estudiante, String estado) {
    return insertar(
        "insert into matriculas(fecha_matricula,estado,periodo_id,estudiante_id) values (?,?,?,?)",
        LocalDate.now(clock),
        estado,
        periodo,
        estudiante);
  }

  private int persona(String dni, String nombre) {
    return insertar(
        "insert into personas(dni,nombre,apellido_paterno,apellido_materno) values (?,?,'Prueba','Panel')",
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
