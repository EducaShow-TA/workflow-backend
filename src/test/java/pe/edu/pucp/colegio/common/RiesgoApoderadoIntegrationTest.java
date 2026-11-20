package pe.edu.pucp.colegio.common;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RiesgoApoderadoIntegrationTest {
  private static final String RUTA = "/api/v1/apoderados/alertas-riesgo-academico";

  @TestConfiguration
  static class Reloj {
    @Bean
    @Primary
    Clock clockRiesgo() {
      return Clock.fixed(Instant.parse("2026-11-20T17:00:00Z"), ZoneId.of("America/Lima"));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  private String sufijo;
  private String usuario;
  private String competencia;
  private int periodo;
  private int matricula;
  private int estudiante;

  @BeforeEach
  void preparar() {
    sufijo = UUID.randomUUID().toString().substring(0, 8);
    usuario = "riesgo-" + sufijo;
    int grado =
        jdbc.queryForObject(
            "select id from grados where numero=1 and nivel_educativo='SECUNDARIA'", Integer.class);
    int seccion =
        insertar(
            "insert into secciones(codigo,vacantes_totales,grado_id) values (?,30,?)",
            "R" + sufijo,
            grado);
    periodo =
        insertar(
            "insert into periodos(fecha_inicio,fecha_fin,estado,grado_id,seccion_id) values ('2026-01-01','2026-12-31','EN_CURSO',?,?)",
            grado,
            seccion);
    int persona = persona("Madre");
    int apoderado = insertar("insert into apoderados(persona_id) values (?)", persona);
    jdbc.update(
        "insert into cuentas_usuario(username,password_hash,rol,persona_id) values (?,'hash','APODERADO',?)",
        usuario,
        persona);
    estudiante = insertar("insert into estudiantes(persona_id) values (?)", persona("Hija"));
    matricula = matricular(estudiante);
    jdbc.update(
        "insert into vinculos_familiares(parentesco,es_principal,apoderado_id,estudiante_id) values ('MADRE',true,?,?)",
        apoderado,
        estudiante);
    String area = "AR" + sufijo;
    competencia = "CP" + sufijo;
    jdbc.update("insert into areas(codigo,nombre) values (?,'Matemática')", area);
    jdbc.update(
        "insert into competencias(codigo,nombre) values (?,'Resuelve problemas')", competencia);
    jdbc.update(
        "insert into areas_competencias(area_codigo,competencia_codigo) values (?,?)",
        area,
        competencia);
    jdbc.update("update configuracion_riesgo_academico set umbral=1 where id=1");
    nota(matricula, 4, false);
    parcial(matricula, false);
    int ajeno = insertar("insert into estudiantes(persona_id) values (?)", persona("Ajeno"));
    int matriculaAjena = matricular(ajeno);
    nota(matriculaAjena, 4, false);
    parcial(matriculaAjena, false);
  }

  @Test
  void devuelveSoloRiesgoDelHijoVinculadoConAreaCompetenciaYUmbral() throws Exception {
    mvc.perform(get(RUTA).with(user(usuario).roles("APODERADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].estudianteId").value(estudiante))
        .andExpect(jsonPath("$[0].bimestre").value(4))
        .andExpect(jsonPath("$[0].areaNombre").value("Matemática"))
        .andExpect(jsonPath("$[0].notasC").value(2))
        .andExpect(jsonPath("$[0].umbral").value(1))
        .andExpect(jsonPath("$[0].competencias[0].nombre").value("Resuelve problemas"));
  }

  @Test
  void desapareceAlCorregirLaNotaYNoCuentaOtrosBimestresNiConsolidaciones() throws Exception {
    jdbc.update("update calificaciones set nota='A' where matricula_id=?", matricula);
    nota(matricula, 3, false);
    jdbc.update(
        "update calificaciones set consolidada=true where matricula_id=? and bimestre=3",
        matricula);
    mvc.perform(get(RUTA).with(user(usuario).roles("APODERADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void ignoraParcialesJustificadasMatriculasRetiradasYVinculosInactivos() throws Exception {
    jdbc.update(
        "update calificaciones_parciales set justificada=true,nota=null where matricula_id=?",
        matricula);
    mvc.perform(get(RUTA).with(user(usuario).roles("APODERADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
    jdbc.update(
        "update calificaciones_parciales set justificada=false,nota='C' where matricula_id=?",
        matricula);
    jdbc.update("update matriculas set estado='RETIRADA' where id=?", matricula);
    mvc.perform(get(RUTA).with(user(usuario).roles("APODERADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
    jdbc.update("update matriculas set estado='CONFIRMADA' where id=?", matricula);
    jdbc.update("update vinculos_familiares set active=false where estudiante_id=?", estudiante);
    mvc.perform(get(RUTA).with(user(usuario).roles("APODERADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void rechazaOtrosRolesYCuentasInactivas() throws Exception {
    mvc.perform(get(RUTA).with(user(usuario).roles("ALUMNO"))).andExpect(status().isForbidden());
    mvc.perform(get(RUTA).with(user(usuario).roles("DOCENTE"))).andExpect(status().isForbidden());
    jdbc.update("update cuentas_usuario set active=false where username=?", usuario);
    mvc.perform(get(RUTA).with(user(usuario).roles("APODERADO"))).andExpect(status().isForbidden());
  }

  private int persona(String nombre) {
    return insertar(
        "insert into personas(dni,nombre,apellido_paterno,apellido_materno) values (?,?,'Prueba','Riesgo')",
        UUID.randomUUID().toString().substring(0, 12),
        nombre);
  }

  private int matricular(int alumno) {
    return insertar(
        "insert into matriculas(fecha_matricula,estado,periodo_id,estudiante_id) values ('2026-01-01','CONFIRMADA',?,?)",
        periodo,
        alumno);
  }

  private void nota(int alumno, int bimestre, boolean consolidada) {
    jdbc.update(
        "insert into calificaciones(periodo_id,matricula_id,competencia_codigo,nota,bimestre,consolidada,fecha_registro) values (?,?,?,'C',?,?,'2026-11-20 10:00:00')",
        periodo,
        alumno,
        competencia,
        bimestre,
        consolidada);
  }

  private void parcial(int alumno, boolean justificada) {
    int evaluacion =
        insertar(
            "insert into evaluaciones_parciales(periodo_id,competencia_codigo,titulo,ponderacion,bimestre) values (?,?,'Evaluación',100,4)",
            periodo,
            competencia);
    jdbc.update(
        "insert into calificaciones_parciales(evaluacion_id,matricula_id,nota,justificada,fecha_registro) values (?,?,'C',?,'2026-11-20 10:00:00')",
        evaluacion,
        alumno,
        justificada);
  }

  private int insertar(String sql, Object... valores) {
    GeneratedKeyHolder llave = new GeneratedKeyHolder();
    jdbc.update(
        conexion -> {
          var sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < valores.length; i++) sentencia.setObject(i + 1, valores[i]);
          return sentencia;
        },
        llave);
    return llave.getKey().intValue();
  }
}
