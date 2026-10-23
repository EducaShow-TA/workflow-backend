package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaRequest;
import pe.edu.pucp.colegio.common.repository.PeriodoMatriculaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "periodo-matricula-test", roles = "ADMINISTRADOR")
@Transactional
class PeriodoMatriculaIntegrationTest {

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Autowired PeriodoMatriculaRepository periodos;

  @Test
  void configuraVentanasYExponeSusEstadosCalculados() throws Exception {
    long ventanasAntes = periodos.countByActiveTrue();
    crear(LocalDate.now().plusDays(2), LocalDate.now().plusDays(7))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    crear(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("VIGENTE"));
    crear(LocalDate.now().minusDays(7), LocalDate.now().minusDays(2))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("VENCIDO"));

    mvc.perform(get("/api/v1/periodos-matricula"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(ventanasAntes + 3));
  }

  @Test
  void rechazaLimiteIgualOAnteriorAlInicio() throws Exception {
    crear(LocalDate.now(), LocalDate.now()).andExpect(status().isBadRequest());
    crear(LocalDate.now().plusDays(2), LocalDate.now().plusDays(1))
        .andExpect(status().isBadRequest());
  }

  @Test
  void restringeConfiguracionAAdministradores() throws Exception {
    mvc.perform(
            post("/api/v1/periodos-matricula")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("docente")
                        .roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaInicio\":\"2026-01-01\",\"fechaLimite\":\"2026-01-10\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void desactivarVentanaLaRetiraDelListadoYConsulta() throws Exception {
    String body =
        crear(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = objectMapper.readTree(body).get("id").asInt();

    mvc.perform(delete("/api/v1/periodos-matricula/{id}", id)).andExpect(status().isNoContent());

    mvc.perform(get("/api/v1/periodos-matricula/{id}", id)).andExpect(status().isNotFound());
  }

  private org.springframework.test.web.servlet.ResultActions crear(
      LocalDate inicio, LocalDate limite) throws Exception {
    return mvc.perform(
        post("/api/v1/periodos-matricula")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PeriodoMatriculaRequest(inicio, limite))));
  }
}
