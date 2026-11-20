package pe.edu.pucp.colegio.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.ConfiguracionMarcajeDocenteRequest;
import pe.edu.pucp.colegio.common.repository.ConfiguracionMarcajeDocenteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "marcaje-config-admin", roles = "ADMINISTRADOR")
@Transactional
class ConfiguracionMarcajeDocenteIntegrationTest {

  private static final String RUTA = "/api/v1/configuracion/marcaje-docente";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ConfiguracionMarcajeDocenteRepository configuraciones;

  @Test
  void obtieneLaConfiguracionGlobalInicialDeQuinceMinutos() throws Exception {
    mvc.perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.toleranciaMinutos").value(15))
        .andExpect(jsonPath("$.ventanaAntesMinutos").value(30))
        .andExpect(jsonPath("$.ventanaDespuesMinutos").value(30));
  }

  @Test
  void actualizaLaUnicaConfiguracionGlobal() throws Exception {
    mvc.perform(
            patch(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ConfiguracionMarcajeDocenteRequest(12, 20, 25))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.toleranciaMinutos").value(12))
        .andExpect(jsonPath("$.ventanaAntesMinutos").value(20))
        .andExpect(jsonPath("$.ventanaDespuesMinutos").value(25));

    mvc.perform(get(RUTA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.toleranciaMinutos").value(12));
    assertThat(configuraciones.count()).isEqualTo(1);
  }

  @Test
  void rechazaToleranciaMayorQueLaVentanaDespuesDelInicio() throws Exception {
    mvc.perform(
            patch(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ConfiguracionMarcajeDocenteRequest(31, 30, 30))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rechazaValoresFueraDelRangoPermitido() throws Exception {
    mvc.perform(
            patch(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ConfiguracionMarcajeDocenteRequest(241, 30, 30))))
        .andExpect(status().isBadRequest());

    mvc.perform(
            patch(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ConfiguracionMarcajeDocenteRequest(15, -1, 30))))
        .andExpect(status().isBadRequest());

    mvc.perform(
            patch(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ConfiguracionMarcajeDocenteRequest(15, 30, 241))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void soloAdministradoresPuedenConsultarLaConfiguracion() throws Exception {
    mvc.perform(get(RUTA).with(user("docente-config").roles("DOCENTE")))
        .andExpect(status().isForbidden());
  }

  @Test
  void soloAdministradoresPuedenModificarLaConfiguracion() throws Exception {
    mvc.perform(
            patch(RUTA)
                .with(user("docente-config").roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ConfiguracionMarcajeDocenteRequest(10, 20, 30))))
        .andExpect(status().isForbidden());
  }
}
