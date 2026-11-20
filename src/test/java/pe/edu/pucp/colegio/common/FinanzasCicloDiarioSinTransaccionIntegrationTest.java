package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import pe.edu.pucp.colegio.common.service.FinanzasService;

@SpringBootTest
class FinanzasCicloDiarioSinTransaccionIntegrationTest {

  @Autowired JdbcTemplate jdbc;
  @Autowired FinanzasService finanzas;
  @Autowired Clock clock;

  @Test
  void generaBoletaYAuditoriaAlProcesarCompromisoSinTransaccionDeTest() {
    String sufijo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    int periodo = 0;
    int matricula = 0;
    int estudiante = 0;
    int apoderado = 0;
    int personaEstudiante = 0;
    int personaApoderado = 0;
    int seccion = 0;
    int compromiso = 0;
    try {
      int grado =
          jdbc.queryForObject(
              "select id from grados where numero=1 and nivel_educativo='SECUNDARIA'",
              Integer.class);
      seccion =
          insertar(
              "insert into secciones(codigo,vacantes_totales,grado_id) values (?,30,?)",
              "CD" + sufijo,
              grado);
      periodo =
          insertar(
              "insert into periodos(fecha_inicio,fecha_fin,estado,grado_id,seccion_id) values (?,?,?,?,?)",
              LocalDate.now(clock).withDayOfMonth(1),
              LocalDate.now(clock).plusMonths(1).withDayOfMonth(1),
              "EN_CURSO",
              grado,
              seccion);
      personaEstudiante = persona("E" + sufijo, "Ciclo");
      estudiante = insertar("insert into estudiantes(persona_id) values (?)", personaEstudiante);
      personaApoderado = persona("A" + sufijo, "Apoderado");
      apoderado = insertar("insert into apoderados(persona_id) values (?)", personaApoderado);
      jdbc.update(
          "insert into vinculos_familiares(parentesco,es_principal,apoderado_id,estudiante_id) values ('MADRE',true,?,?)",
          apoderado,
          estudiante);
      matricula =
          insertar(
              "insert into matriculas(fecha_matricula,estado,periodo_id,estudiante_id) values (?,?,?,?)",
              LocalDate.now(clock),
              "CONFIRMADA",
              periodo,
              estudiante);
      LocalDate vencimiento = LocalDate.now(clock).withDayOfMonth(20);
      YearMonth mes = YearMonth.from(vencimiento);
      compromiso =
          insertar(
              "insert into compromisos_pago(matricula_id,apoderado_id,concepto,monto_total,fecha_emision,mes_inicio,numero_cuotas,primer_vencimiento,dia_vencimiento) values (?,?,?,?,?,?,?,?,?)",
              matricula,
              apoderado,
              "Ciclo diario " + sufijo,
              "600.00",
              LocalDate.now(clock),
              mes.atDay(1),
              2,
              vencimiento,
              20);

      finanzas.procesarCicloDiario();

      assertEquals(
          1,
          jdbc.queryForObject(
              "select count(*) from boletas_pension where compromiso_id=? and mes_cobro=? and active=true",
              Integer.class,
              compromiso,
              mes.atDay(1)));
      assertEquals(
          1,
          jdbc.queryForObject(
              "select count(*) from auditoria_finanzas where compromiso_id=? and accion='BOLETA_GENERADA'",
              Integer.class,
              compromiso));
    } finally {
      if (compromiso != 0) {
        jdbc.update("delete from auditoria_finanzas where compromiso_id=?", compromiso);
        jdbc.update("delete from boletas_pension where compromiso_id=?", compromiso);
        jdbc.update("delete from compromisos_pago where id=?", compromiso);
      }
      if (estudiante != 0) {
        jdbc.update("delete from vinculos_familiares where estudiante_id=?", estudiante);
        jdbc.update("delete from matriculas where id=?", matricula);
        jdbc.update("delete from estudiantes where id=?", estudiante);
      }
      if (apoderado != 0) jdbc.update("delete from apoderados where id=?", apoderado);
      if (periodo != 0) jdbc.update("delete from periodos where id=?", periodo);
      if (seccion != 0) jdbc.update("delete from secciones where id=?", seccion);
      if (personaEstudiante != 0) jdbc.update("delete from personas where id=?", personaEstudiante);
      if (personaApoderado != 0) jdbc.update("delete from personas where id=?", personaApoderado);
    }
  }

  private int persona(String dni, String nombre) {
    return insertar(
        "insert into personas(dni,nombre,apellido_paterno,apellido_materno) values (?,?,'Prueba','Ciclo')",
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
