package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.CompetenciaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "catalogo-test", roles = "ADMINISTRADOR")
class CatalogoCrudIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AreaRepository areas;
  @Autowired CompetenciaRepository competencias;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void limpiarCatalogo() {
    jdbc.update("DELETE FROM areas_competencias");
    competencias.deleteAll();
    areas.deleteAll();
  }

  @Test
  void areaSeRelacionaConVariasCompetenciasActualizaVinculosYSeEliminaSinBorrarlas()
      throws Exception {
    crearCompetencia("MAT-01", "Resuelve problemas");
    crearCompetencia("MAT-02", "Gestiona datos");

    mvc.perform(
            post("/api/v1/areas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(areaJson("MAT", "Matemática", "Área curricular", "MAT-01", "MAT-02")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.competenciaCodigos.length()").value(2));

    mvc.perform(get("/api/v1/areas/MAT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.competenciaCodigos.length()").value(2));
    mvc.perform(get("/api/v1/competencias/MAT-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaCodigos[0]").value("MAT"));
    mvc.perform(get("/api/v1/competencias/MAT-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaCodigos[0]").value("MAT"));

    mvc.perform(
            put("/api/v1/areas/MAT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(areaJson("MAT", "Matemática actualizada", null, "MAT-02")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Matemática actualizada"))
        .andExpect(jsonPath("$.competenciaCodigos.length()").value(1))
        .andExpect(jsonPath("$.competenciaCodigos[0]").value("MAT-02"));
    mvc.perform(get("/api/v1/competencias/MAT-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaCodigos").isEmpty());
    mvc.perform(get("/api/v1/competencias/MAT-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaCodigos[0]").value("MAT"));

    mvc.perform(delete("/api/v1/areas/MAT")).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/areas/MAT")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/competencias/MAT-02"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaCodigos").isEmpty());
  }

  @Test
  void crudDeAreaPersisteSusDatosYAsociaciones() throws Exception {
    crearCompetencia("MAT-01", "Resuelve problemas");

    mvc.perform(
            post("/api/v1/areas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(areaJson("MAT", "Matemática", "Área curricular", "MAT-01")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigo").value("MAT"))
        .andExpect(jsonPath("$.competenciaCodigos[0]").value("MAT-01"));

    mvc.perform(get("/api/v1/areas/MAT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Matemática"));

    mvc.perform(
            put("/api/v1/areas/MAT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(areaJson("MAT", "Matemática actualizada", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Matemática actualizada"))
        .andExpect(jsonPath("$.competenciaCodigos").isEmpty());

    mvc.perform(delete("/api/v1/areas/MAT")).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/areas/MAT")).andExpect(status().isNotFound());
  }

  @Test
  void competenciaPuedePertenecerAMultiplesAreasYSeActualizaLaRelacion() throws Exception {
    crearArea("MAT", "Matemática");
    crearArea("CIEN", "Ciencia y Tecnología");

    mvc.perform(
            post("/api/v1/competencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(competenciaJson("INV", "Indaga", "MAT", "CIEN")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.areaCodigos.length()").value(2));

    mvc.perform(get("/api/v1/areas/MAT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.competenciaCodigos[0]").value("INV"));
    mvc.perform(get("/api/v1/areas/CIEN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.competenciaCodigos[0]").value("INV"));

    mvc.perform(
            put("/api/v1/competencias/INV")
                .contentType(MediaType.APPLICATION_JSON)
                .content(competenciaJson("INV", "Indaga", "CIEN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaCodigos.length()").value(1))
        .andExpect(jsonPath("$.areaCodigos[0]").value("CIEN"));

    mvc.perform(get("/api/v1/areas/MAT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.competenciaCodigos").isEmpty());
  }

  @Test
  void crudDeCompetenciaYEliminacionDesvinculanSinBorrarAreas() throws Exception {
    crearArea("COM", "Comunicación");

    mvc.perform(
            post("/api/v1/competencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(competenciaJson("LEE", "Lee", "COM")))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/v1/competencias/LEE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Lee"));
    mvc.perform(
            put("/api/v1/competencias/LEE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(competenciaJson("LEE", "Lee y comprende", "COM")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Lee y comprende"));

    mvc.perform(delete("/api/v1/competencias/LEE")).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/competencias/LEE")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/areas/COM")).andExpect(status().isOk());
  }

  @Test
  void rechazaReferenciasInexistentesDatosInvalidosYCodigosInconsistentes() throws Exception {
    mvc.perform(
            post("/api/v1/areas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(areaJson("MAT", "Matemática", null, "NO-EXISTE")))
        .andExpect(status().isBadRequest());

    mvc.perform(
            post("/api/v1/competencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(competenciaJson("COMP", "", "NO-EXISTE")))
        .andExpect(status().isBadRequest());

    mvc.perform(
            put("/api/v1/areas/MAT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(areaJson("OTRO", "Matemática", null)))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/v1/areas/NO-EXISTE")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/competencias/NO-EXISTE")).andExpect(status().isNotFound());
  }

  private void crearArea(String codigo, String nombre) throws Exception {
    mvc.perform(
            post("/api/v1/areas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(areaJson(codigo, nombre, null)))
        .andExpect(status().isCreated());
  }

  private void crearCompetencia(String codigo, String nombre) throws Exception {
    mvc.perform(
            post("/api/v1/competencias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(competenciaJson(codigo, nombre)))
        .andExpect(status().isCreated());
  }

  private String areaJson(
      String codigo, String nombre, String descripcion, String... competenciaCodigos) {
    return "{\"codigo\":\""
        + codigo
        + "\",\"nombre\":\""
        + nombre
        + "\",\"descripcion\":"
        + (descripcion == null ? "null" : "\"" + descripcion + "\"")
        + ",\"competenciaCodigos\":"
        + stringArray(competenciaCodigos)
        + "}";
  }

  private String competenciaJson(String codigo, String nombre, String... areaCodigos) {
    return "{\"codigo\":\""
        + codigo
        + "\",\"nombre\":\""
        + nombre
        + "\",\"descripcion\":\"Descripción de prueba\",\"areaCodigos\":"
        + stringArray(areaCodigos)
        + "}";
  }

  private String stringArray(String... values) {
    if (values == null || values.length == 0) return "[]";
    return "[\"" + String.join("\",\"", values) + "\"]";
  }
}
