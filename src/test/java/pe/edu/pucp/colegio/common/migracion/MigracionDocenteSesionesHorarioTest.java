package pe.edu.pucp.colegio.common.migracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * V32 pasa sesiones_horario.docente_id de cuentas_usuario.id a docentes.id. Se prueba con datos
 * cargados antes de la migracion, como los que ya existen en la RDS.
 */
class MigracionDocenteSesionesHorarioTest {

  @Test
  void migrarV32_asignacionesConIdDeCuenta_apuntanAlDocenteDeLaMismaPersona() throws Exception {
    String url = "jdbc:h2:mem:v32-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
    migrarHasta(url, "31");

    try (Connection conexion = DriverManager.getConnection(url, "sa", "");
        Statement sql = conexion.createStatement()) {
      // Solo interesan docente_id y sus personas: se omiten horario, bloque, area y aula.
      sql.execute("SET REFERENTIAL_INTEGRITY FALSE");
      sql.execute(
          "INSERT INTO personas (id, active, dni, nombre, apellido_paterno, apellido_materno,"
              + " genero) VALUES"
              + " (9001, TRUE, '90000001', 'Marta', 'Quispe', 'Salas', 'NO_IDENTIFICADO'),"
              + " (9002, TRUE, '90000002', 'Luis', 'Rojas', 'Luna', 'NO_IDENTIFICADO')");
      sql.execute(
          "INSERT INTO cuentas_usuario (id, active, username, password_hash, rol,"
              + " intentos_fallidos, persona_id) VALUES"
              + " (9101, TRUE, 'docente9001', 'hash', 'DOCENTE', 0, 9001),"
              + " (9102, TRUE, 'docente9002', 'hash', 'DOCENTE', 0, 9002)");
      // La persona 9002 ya tenia perfil docente; la 9001 todavia no.
      sql.execute("INSERT INTO docentes (id, active, persona_id) VALUES (9201, TRUE, 9002)");
      sql.execute(
          "INSERT INTO sesiones_horario (id, horario_id, bloque_id, dia, area_codigo, docente_id,"
              + " aula_id, vigente_desde) VALUES"
              + " (9301, 1, 1, 'MONDAY', 'AREA', 9101, 1, '2026-01-01'),"
              + " (9302, 1, 1, 'TUESDAY', 'AREA', 9102, 1, '2026-01-01')");
      sql.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    migrarHasta(url, "32");

    try (Connection conexion = DriverManager.getConnection(url, "sa", "");
        Statement sql = conexion.createStatement()) {
      Integer docenteNuevo = entero(sql, "SELECT id FROM docentes WHERE persona_id = 9001");
      assertThat(docenteNuevo).isNotNull();
      assertThat(entero(sql, "SELECT docente_id FROM sesiones_horario WHERE id = 9301"))
          .isEqualTo(docenteNuevo);
      assertThat(entero(sql, "SELECT docente_id FROM sesiones_horario WHERE id = 9302"))
          .isEqualTo(9201);
      assertThat(entero(sql, "SELECT COUNT(*) FROM docentes WHERE persona_id = 9002")).isEqualTo(1);
    }
  }

  private void migrarHasta(String url, String version) {
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target(version)
        .load()
        .migrate();
  }

  private Integer entero(Statement sql, String consulta) throws SQLException {
    try (ResultSet fila = sql.executeQuery(consulta)) {
      return fila.next() ? (Integer) fila.getObject(1, Integer.class) : null;
    }
  }
}
