package pe.edu.pucp.colegio.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"app.demo.fecha-permitida=false", "app.pagos.simulado.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class ParametrosSistemaDeshabilitadoIntegrationTest {

  private static final String RUTA = "/api/v1/parametros-sistema";

  @Autowired MockMvc mvc;

  @Test
  void sinLaVariableDelServidorNoSePuedeSimularLaFecha() throws Exception {
    mvc.perform(
            put(RUTA + "/fecha")
                .with(user("admin-off").roles("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fecha\":\"2026-04-01T07:50:00\",\"motivo\":\"Demostración del sistema\","
                        + "\"contrasenia\":\"ClaveDemo123!\"}"))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message").value("La fecha simulada no está habilitada en este servidor"));

    mvc.perform(get(RUTA).with(user("admin-off").roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaSimuladaPermitida").value(false))
        .andExpect(jsonPath("$.fechaSimulada").value(false))
        .andExpect(jsonPath("$.pagoEnLineaPermitido").value(false));
  }

  @Test
  void sinPermisoDelServidorElPagoEnLineaNoSeActiva() throws Exception {
    mvc.perform(
            put(RUTA + "/pago-en-linea")
                .with(user("admin-off").roles("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\":true}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("El servidor no permite el pago en línea simulado"));
    mvc.perform(get(RUTA + "/estado").with(user("apo-off").roles("APODERADO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pagoEnLineaDisponible").value(false));
  }
}
