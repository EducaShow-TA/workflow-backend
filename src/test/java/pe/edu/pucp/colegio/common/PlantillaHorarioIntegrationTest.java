package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "plantilla-horario-test", roles = "ADMINISTRADOR")
class PlantillaHorarioIntegrationTest {

  @Autowired MockMvc mvc;

  @Test
  void rechazaBloquesQueExcedenElTurno() throws Exception {
    crear(
            """
            {"nombre":"Excede","turno":"TARDE","horaInicio":"13:00:00","horaFin":"16:00:00",
            "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":5,"recreos":[]}
            """)
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaDosRecreosDespuesDelMismoBloque() throws Exception {
    crear(
            """
            {"nombre":"Recreos duplicados","turno":"MANANA","horaInicio":"08:00:00","horaFin":"13:00:00",
            "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":4,
            "recreos":[{"despuesDeBloque":2,"duracionMinutos":10},{"despuesDeBloque":2,"duracionMinutos":15}]}
            """)
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaRecreoDespuesDeBloqueInexistente() throws Exception {
    crear(
            """
            {"nombre":"Recreo fuera de rango","turno":"MANANA","horaInicio":"08:00:00","horaFin":"13:00:00",
            "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":2,
            "recreos":[{"despuesDeBloque":3,"duracionMinutos":10}]}
            """)
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaTurnoMananaQueEmpiezaPorLaTarde() throws Exception {
    crear(
            """
            {"nombre":"Mañana incoherente","turno":"MANANA","horaInicio":"15:00:00","horaFin":"18:00:00",
            "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":2,"recreos":[]}
            """)
        .andExpect(status().isUnprocessableEntity());
  }

  private org.springframework.test.web.servlet.ResultActions crear(String cuerpo) throws Exception {
    return mvc.perform(
        post("/api/v1/horarios/plantillas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));
  }
}
