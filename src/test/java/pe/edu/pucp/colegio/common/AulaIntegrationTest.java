package pe.edu.pucp.colegio.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "aula-test", roles = "ADMINISTRADOR")
class AulaIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void creaAulaYLaConsulta() throws Exception {
        crear("{\"codigo\":\"A-101\",\"capacidad\":35,\"descripcion\":\"Primer piso\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("A-101"))
                .andExpect(jsonPath("$.capacidad").value(35))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void rechazaCodigoDuplicadoSinDistinguirMayusculas() throws Exception {
        crear("{\"codigo\":\"B-201\",\"capacidad\":30}").andExpect(status().isCreated());
        crear("{\"codigo\":\"b-201\",\"capacidad\":25}").andExpect(status().isConflict());
    }

    @Test
    void rechazaCapacidadInvalida() throws Exception {
        crear("{\"codigo\":\"C-301\",\"capacidad\":0}").andExpect(status().isBadRequest());
        crear("{\"codigo\":\"C-302\",\"capacidad\":500}").andExpect(status().isBadRequest());
    }

    @Test
    void rechazaCodigoConCaracteresInvalidos() throws Exception {
        crear("{\"codigo\":\"Aula #1\",\"capacidad\":30}").andExpect(status().isBadRequest());
    }

    @Test
    void actualizaCapacidadYDescripcion() throws Exception {
        int id = idDe(crear("{\"codigo\":\"D-401\",\"capacidad\":30}"));
        mvc.perform(
                        put("/api/v1/aulas/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"codigo\":\"D-401\",\"capacidad\":40,\"descripcion\":\"Ampliada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidad").value(40))
                .andExpect(jsonPath("$.descripcion").value("Ampliada"));
    }

    @Test
    void desactivaYReactivaAula() throws Exception {
        int id = idDe(crear("{\"codigo\":\"E-501\",\"capacidad\":30}"));
        cambiarEstado(id, false).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        cambiarEstado(id, true).andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void devuelve404ParaAulaInexistente() throws Exception {
        mvc.perform(get("/api/v1/aulas/999999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "docente-test", roles = "DOCENTE")
    void rechazaUsuariosSinRolAdministrador() throws Exception {
        mvc.perform(get("/api/v1/aulas")).andExpect(status().isForbidden());
    }

    private ResultActions crear(String cuerpo) throws Exception {
        return mvc.perform(post("/api/v1/aulas").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions cambiarEstado(int id, boolean activo) throws Exception {
        return mvc.perform(
                patch("/api/v1/aulas/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":" + activo + "}"));
    }

    private int idDe(ResultActions creacion) throws Exception {
        creacion.andExpect(status().isCreated());
        String cuerpo = creacion.andReturn().getResponse().getContentAsString();
        JsonNode nodo = json.readTree(cuerpo);
        return nodo.get("id").asInt();
    }
}
