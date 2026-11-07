package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "seccion-test", roles = "ADMINISTRADOR")
@Transactional
class SeccionIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired GradoRepository grados;
  @Autowired SeccionRepository secciones;

  private String codigoLibre() {
    String letras = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    var random = ThreadLocalRandom.current();
    Integer gradoId =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(1, NivelEducativo.SECUNDARIA)
            .orElseThrow()
            .getId();
    for (int intento = 0; intento < 100; intento++) {
      String codigo =
          ""
              + letras.charAt(random.nextInt(letras.length()))
              + letras.charAt(random.nextInt(letras.length()));
      if (!secciones.existsByGradoIdAndCodigoIgnoreCase(gradoId, codigo)) {
        return codigo;
      }
    }
    throw new IllegalStateException("No se encontró un código libre para la sección de prueba");
  }

  private ResultActions crear(String nombre, int vacantes) throws Exception {
    return mvc.perform(
        post("/api/v1/secciones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"gradoNumero\":1,\"nombre\":\""
                    + nombre
                    + "\",\"vacantesTotales\":"
                    + vacantes
                    + "}"));
  }

  @Test
  void listar_retorna200() throws Exception {
    mvc.perform(get("/api/v1/secciones")).andExpect(status().isOk());
  }

  @Test
  void crear_datosValidos_retorna201ConVacantesDisponibles() throws Exception {
    crear(codigoLibre(), 30)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.vacantesTotales").value(30))
        .andExpect(jsonPath("$.cantidadMatriculados").value(0))
        .andExpect(jsonPath("$.vacantesDisponibles").value(30));
  }

  @Test
  void crear_nombreRepetidoEnElGrado_retorna409() throws Exception {
    String nombre = codigoLibre();
    crear(nombre, 30).andExpect(status().isCreated());
    crear(nombre, 30).andExpect(status().isConflict());
  }

  @Test
  void crear_capacidadNoPositiva_retorna400() throws Exception {
    crear(codigoLibre(), 0).andExpect(status().isBadRequest());
  }

  @Test
  void crear_nombreEnMinusculas_retorna400() throws Exception {
    crear("ab", 30).andExpect(status().isBadRequest());
  }

  @Test
  void actualizarVacantes_capacidadValida_retorna200() throws Exception {
    String cuerpo =
        crear(codigoLibre(), 30)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = json.readTree(cuerpo).get("id").asInt();

    mvc.perform(
            patch("/api/v1/secciones/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vacantesTotales\":35}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vacantesTotales").value(35))
        .andExpect(jsonPath("$.vacantesDisponibles").value(35));
  }

  @Test
  void actualizarVacantes_seccionInexistente_retorna404() throws Exception {
    mvc.perform(
            patch("/api/v1/secciones/{id}", Integer.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vacantesTotales\":10}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void actualizarVacantes_capacidadNoPositiva_retorna400() throws Exception {
    String cuerpo =
        crear(codigoLibre(), 30)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    int id = json.readTree(cuerpo).get("id").asInt();

    mvc.perform(
            patch("/api/v1/secciones/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vacantesTotales\":0}"))
        .andExpect(status().isBadRequest());
  }
}
