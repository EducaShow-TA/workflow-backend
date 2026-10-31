package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin-test", roles = "ADMINISTRADOR")
class EstudianteControllerIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired PersonaRepository personas;
  @Autowired EstudianteRepository estudiantes;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired VinculoFamiliarRepository vinculos;

  @Test
  void crearDaDeAltaCuentaConRolAlumnoYPerfilEstudiante() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String username = "alumno" + suffix;
    String dni =
        String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000));
    String dniApoderado =
        String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000));
    String dniSegundoApoderado =
        String.format("%08d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000));

    String cuerpo =
        """
        {"username":"%s","password":"ClaveSegura123","tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Ana","apellidoPaterno":"Pérez","apellidoMaterno":"Díaz","correoElectronico":"ana-%s@ejemplo.edu.pe","fechaNacimiento":"2012-05-10","apoderados":[{"tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Rosa","apellidoPaterno":"Quispe","apellidoMaterno":"Mamani","celular":"987654321","parentesco":"MADRE","esPrincipal":true},{"tipoDocumento":"DNI","numeroDocumento":"%s","nombres":"Juan","apellidoPaterno":"Quispe","apellidoMaterno":"Mamani","celular":"987654320","parentesco":"PADRE","esPrincipal":false}]}
        """
            .formatted(username, dni, suffix, dniApoderado, dniSegundoApoderado);

    mvc.perform(
            post("/api/v1/estudiantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo.replace(username, "alumno" + suffix + "@ejemplo.edu.pe")))
        .andExpect(status().isBadRequest());

    mvc.perform(post("/api/v1/estudiantes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value(username))
        .andExpect(jsonPath("$.rol").value("ALUMNO"))
        .andExpect(jsonPath("$.dni").value(dni));
    assertEquals(
        2, vinculos.countByEstudianteId(estudiantes.findByPersonaDni(dni).orElseThrow().getId()));

    CuentaUsuario cuenta = cuentas.findByUsername(username).orElseThrow();
    assertEquals(Rol.ALUMNO, cuenta.getRol());
    assertNotEquals("ClaveSegura123", cuenta.getPassword());
    assertTrue(passwordEncoder.matches("ClaveSegura123", cuenta.getPassword()));
    assertTrue(estudiantes.existsByPersonaId(cuenta.getPersona().getId()));
    assertEquals(
        "ana-" + suffix + "@ejemplo.edu.pe",
        personas.findById(cuenta.getPersona().getId()).orElseThrow().getCorreo());

    mvc.perform(
            get("/api/v1/estudiantes")
                .param("nombre", "Ana")
                .param("dni", dni)
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].dni").value(dni))
        .andExpect(jsonPath("$.content[0].correo").value("ana-" + suffix + "@ejemplo.edu.pe"))
        .andExpect(jsonPath("$.size").value(1));
  }

  @Test
  void asignarCambiaARolAlumnoYVinculaPerfilConPersonaDeCuentaExistente() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Persona persona = new Persona("2" + suffix.replace("-", ""), "Luis", "Rojas", "Luna");
    persona = personas.save(persona);

    CuentaUsuario cuenta =
        new CuentaUsuario(
            "existente-" + suffix, passwordEncoder.encode("ClaveSegura123"), Rol.DOCENTE, persona);
    cuenta = cuentas.save(cuenta);
    Integer cuentaId = cuenta.getId();
    Integer personaId = persona.getId();

    mvc.perform(post("/api/v1/estudiantes/{id}/asignar", cuentaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rol").value("ALUMNO"))
        .andExpect(jsonPath("$.personaId").value(personaId));

    assertEquals(Rol.ALUMNO, cuentas.findById(cuentaId).orElseThrow().getRol());
    Estudiante estudiante =
        estudiantes.findAll().stream()
            .filter(item -> item.getPersona().getId().equals(personaId))
            .findFirst()
            .orElseThrow();
    assertEquals(personaId, estudiante.getPersona().getId());
  }
}
