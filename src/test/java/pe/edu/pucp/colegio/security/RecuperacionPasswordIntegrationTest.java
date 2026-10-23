package pe.edu.pucp.colegio.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.model.TokenRecuperacionPassword;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.repository.TokenRecuperacionPasswordRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RecuperacionPasswordIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired PersonaRepository personas;
  @Autowired TokenRecuperacionPasswordRepository tokens;
  @Autowired PasswordEncoder passwordEncoder;

  @MockitoBean JavaMailSender mailSender;

  @Test
  @Transactional
  void solicitaYConsumeTokenParaCambiarPasswordUnaSolaVez() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String correo = "recuperacion-" + suffix + "@ejemplo.edu.pe";
    CuentaUsuario cuenta = crearCuenta(correo, suffix);

    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + correo + "\"}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString("Si la cuenta existe")));

    var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    SimpleMailMessage mensaje = captor.getValue();
    assertTrue(java.util.Arrays.asList(mensaje.getTo()).contains(correo));

    String tokenClaro = extraerToken(mensaje.getText());
    TokenRecuperacionPassword tokenGuardado =
        tokens.findByCuentaUsuarioAndActiveTrue(cuenta).getFirst();
    assertFalse(tokenGuardado.getTokenHash().equals(tokenClaro));
    assertTrue(tokenGuardado.getTokenHash().matches("[0-9a-f]{64}"));

    for (int intento = 0; intento < 5; intento++) {
      cuenta.registrarIntentoFallido(5, 30);
    }
    cuentas.save(cuenta);
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + correo + "\",\"password\":\"ClaveOriginal123\"}"))
        .andExpect(status().isUnauthorized());

    mvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + tokenClaro + "\",\"password\":\"NuevaClaveSegura123\"}"))
        .andExpect(status().isOk());

    CuentaUsuario actualizada = cuentas.findById(cuenta.getId()).orElseThrow();
    assertTrue(passwordEncoder.matches("NuevaClaveSegura123", actualizada.getPassword()));
    assertFalse(tokens.findById(tokenGuardado.getId()).orElseThrow().getActive());
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + correo + "\",\"password\":\"NuevaClaveSegura123\"}"))
        .andExpect(status().isOk());

    mvc.perform(
            post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + tokenClaro + "\",\"password\":\"OtraClaveSegura123\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void solicitudParaUsernameInexistenteRespondeGenericoSinEnviarCorreo() throws Exception {
    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"noexiste@ejemplo.edu.pe\"}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.message")
                .value(org.hamcrest.Matchers.containsString("Si la cuenta existe")));
    verifyNoInteractions(mailSender);
  }

  @Test
  void solicitudConUsernameNoEmailUsaCorreoDePersona() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String correo = "alumno-" + suffix + "@ejemplo.edu.pe";
    Persona persona = new Persona("8" + suffix, "Alumno", "Apellido", "Prueba");
    persona.setCorreo(correo);
    persona = personas.save(persona);
    cuentas.save(
        new CuentaUsuario(
            "alumno" + suffix, passwordEncoder.encode("ClaveOriginal123"), Rol.ALUMNO, persona));

    mvc.perform(
            post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alumno" + suffix + "\"}"))
        .andExpect(status().isOk());

    var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    assertTrue(java.util.Arrays.asList(captor.getValue().getTo()).contains(correo));
  }

  private CuentaUsuario crearCuenta(String correo, String suffix) {
    Persona persona = new Persona("9" + suffix, "Nombre", "Apellido", "Prueba");
    persona.setCorreo(correo);
    persona = personas.save(persona);
    return cuentas.save(
        new CuentaUsuario(correo, passwordEncoder.encode("ClaveOriginal123"), Rol.ALUMNO, persona));
  }

  private String extraerToken(String textoCorreo) {
    String enlace =
        java.util.Arrays.stream(textoCorreo.split("\\R"))
            .filter(linea -> linea.startsWith("http://"))
            .findFirst()
            .orElseThrow();
    return URI.create(enlace)
        .getRawQuery()
        .substring("token=".length())
        .transform(valor -> URLDecoder.decode(valor, StandardCharsets.UTF_8));
  }
}
