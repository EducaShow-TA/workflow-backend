package pe.edu.pucp.colegio.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired PersonaRepository personas;

  @Test
  void loginDelAdministradorRetornaJwtYPerfil() throws Exception {
    assertEquals("admin01", cuentas.findById(1).orElseThrow().getUsername());
    assertEquals("admin@educore.edu.pe", personas.findByDni("00000000").orElseThrow().getCorreo());

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin@educore.edu.pe\",\"password\":\"Admin2026!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.usuario.rol").value("ADMINISTRADOR"))
        .andExpect(jsonPath("$.usuario.username").value("admin01"));

    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin01\",\"password\":\"Admin2026!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuario.username").value("admin01"));
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
  void distingueUsuarioInexistenteDeCuentaDesactivadaSoloConClaveCorrecta() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"no-existe-qa\",\"password\":\"Incorrecta\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("No encontramos una cuenta con ese usuario o correo."));

    CuentaUsuario admin = cuentas.findById(1).orElseThrow();
    boolean activoOriginal = admin.getActive();
    admin.setActive(false);
    cuentas.saveAndFlush(admin);
    try {
      mvc.perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"admin@educore.edu.pe\",\"password\":\"Incorrecta\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value("Credenciales inválidas"));

      mvc.perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"username\":\"admin@educore.edu.pe\",\"password\":\"Admin2026!\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message").value(
              "Esta cuenta se encuentra desactivada. Contacta al administrador del colegio."));
    } finally {
      admin.setActive(activoOriginal);
      cuentas.saveAndFlush(admin);
    }
  }

  @Test
  void rutaProtegidaRechazaSolicitudSinToken() throws Exception {
    mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void tercerIntentoFallidoPersisteBloqueoYRechazaClaveCorrecta() throws Exception {
    CuentaUsuario admin = cuentas.findById(1).orElseThrow();
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
          .andExpect(jsonPath("$.message").value("Tu cuenta está bloqueada temporalmente. Intenta más tarde."));
    } finally {
      admin.reiniciarIntentos();
      cuentas.save(admin);
    }
  }
}
