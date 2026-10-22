package pe.edu.pucp.colegio.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired CuentaUsuarioRepository cuentas;

  @Test
  void loginDelAdministradorRetornaJwtYPerfil() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin@educore.edu.pe\",\"password\":\"Admin2026!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.usuario.rol").value("ADMINISTRADOR"));
  }

  @Test
  void credencialesInvalidasRetornan401() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin@educore.edu.pe\",\"password\":\"Incorrecta\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
  }

  @Test
  void rutaProtegidaRechazaSolicitudSinToken() throws Exception {
    mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void tercerIntentoFallidoPersisteBloqueoYRechazaClaveCorrecta() throws Exception {
    CuentaUsuario admin = cuentas.findByUsername("admin@educore.edu.pe").orElseThrow();
    admin.reiniciarIntentos();
    cuentas.save(admin);
    try {
      for (int intento = 0; intento < 3; intento++) {
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"admin@educore.edu.pe\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized());
      }
      mvc.perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"admin@educore.edu.pe\",\"password\":\"Admin2026!\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    } finally {
      admin.reiniciarIntentos();
      cuentas.save(admin);
    }
  }
}
