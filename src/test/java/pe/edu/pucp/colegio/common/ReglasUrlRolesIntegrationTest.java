package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ReglasUrlRolesIntegrationTest {

  @Autowired MockMvc mvc;

  @Test
  @WithMockUser(username = "docente-url-test", roles = "DOCENTE")
  void docentePuedeAccederASuHorario() throws Exception {
    MvcResult result =
        mvc.perform(get("/api/v1/docentes/mi-horario").param("anio", "2026")).andReturn();
    assertControladorAlcanzaServicio(result, "La cuenta no corresponde a un docente activo");
  }

  @Test
  @WithMockUser(username = "apoderado-url-test", roles = "APODERADO")
  void apoderadoPuedeConsultarSusEstudiantes() throws Exception {
    MvcResult result = mvc.perform(get("/api/v1/apoderados/mis-estudiantes")).andReturn();
    assertControladorAlcanzaServicio(result, "La cuenta no corresponde a un apoderado activo");
  }

  @Test
  @WithMockUser(username = "docente-url-test", roles = "DOCENTE")
  void docentePuedeConsultarEvaluacionesParciales() throws Exception {
    assertNoDenegadoPorMatcher(
        mvc.perform(
                get("/api/v1/evaluaciones-parciales")
                    .param("periodoId", "1")
                    .param("competenciaCodigo", "MAT"))
            .andReturn());
  }

  @Test
  @WithMockUser(username = "docente-url-test", roles = "DOCENTE")
  void putDeNotaParcialLlegaALaAutorizacionDelControlador() throws Exception {
    assertNoDenegadoPorMatcher(
        mvc.perform(
                put("/api/v1/evaluaciones-parciales/1/matriculas/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nota\":\"A\",\"justificada\":false}"))
            .andReturn());
  }

  private void assertControladorAlcanzaServicio(MvcResult result, String mensajeServicio)
      throws Exception {
    assertTrue(result.getResponse().getContentAsString().contains(mensajeServicio));
  }

  private void assertNoDenegadoPorMatcher(MvcResult result) {
    assertNotEquals(403, result.getResponse().getStatus());
  }
}
