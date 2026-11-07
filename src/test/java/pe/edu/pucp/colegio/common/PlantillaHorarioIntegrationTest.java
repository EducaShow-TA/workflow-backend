package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
  void creaPlantillaYCalculaBloquesYRecreos() throws Exception {
    crear(
            """
            {"nombre":"Jornada mañana feliz","turno":"MANANA","horaInicio":"07:30:00","horaFin":"13:00:00",
            "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":6,
            "recreos":[{"despuesDeBloque":2,"duracionMinutos":15},{"despuesDeBloque":4,"duracionMinutos":20}]}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bloques.length()").value(8))
        .andExpect(jsonPath("$.bloques[0].horaInicio").value("07:30:00"))
        .andExpect(jsonPath("$.bloques[0].horaFin").value("08:15:00"))
        .andExpect(jsonPath("$.bloques[1].horaInicio").value("08:15:00"))
        .andExpect(jsonPath("$.bloques[1].horaFin").value("09:00:00"))
        .andExpect(jsonPath("$.bloques[2].tipo").value("RECREO"))
        .andExpect(jsonPath("$.bloques[2].horaInicio").value("09:00:00"))
        .andExpect(jsonPath("$.bloques[2].horaFin").value("09:15:00"))
        .andExpect(jsonPath("$.bloques[5].tipo").value("RECREO"))
        .andExpect(jsonPath("$.bloques[5].horaInicio").value("10:45:00"))
        .andExpect(jsonPath("$.bloques[5].horaFin").value("11:05:00"))
        .andExpect(jsonPath("$.bloques[7].horaInicio").value("11:50:00"))
        .andExpect(jsonPath("$.bloques[7].horaFin").value("12:35:00"));
  }

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
