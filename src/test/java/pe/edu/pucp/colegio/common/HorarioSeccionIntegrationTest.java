package pe.edu.pucp.colegio.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.PlantillaHorario;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.PlantillaHorarioRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "horario-seccion-test", roles = "ADMINISTRADOR")
@Transactional
class HorarioSeccionIntegrationTest {

  private static final int ANIO = LocalDate.now().getYear() + 1;
  private static final String BASE = "/api/v1/horarios/secciones";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired GradoRepository grados;
  @Autowired SeccionRepository secciones;
  @Autowired AreaRepository areas;
  @Autowired AulaRepository aulas;
  @Autowired PersonaRepository personas;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired DocenteRepository docentes;
  @Autowired EstudianteRepository estudiantes;
  @Autowired pe.edu.pucp.colegio.common.repository.PeriodoRepository periodos;
  @Autowired pe.edu.pucp.colegio.common.repository.MatriculaRepository matriculas;
  @Autowired PlantillaHorarioRepository plantillas;
  @Autowired SesionHorarioRepository sesionesHorario;

  /** Datos mínimos que necesita un horario: sección, plantilla, área, docente y aula. */
  record Escenario(
      int seccionId,
      int plantillaId,
      int bloque1Id,
      int bloque2Id,
      int recreoId,
      String areaCodigo,
      int docenteId,
      int cuentaDocenteId,
      int aulaId,
      String sufijo) {}

  @Test
  void creaHorarioYDevuelveGrillaConBloquesYRecreos() throws Exception {
    Escenario e = escenario();
    crearHorario(e)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.anio").value(ANIO))
        .andExpect(jsonPath("$.turno").value("MANANA"))
        .andExpect(jsonPath("$.dias.length()").value(5))
        .andExpect(jsonPath("$.dias[0]").value("MONDAY"))
        .andExpect(jsonPath("$.dias[4]").value("FRIDAY"))
        .andExpect(jsonPath("$.bloques.length()").value(8))
        .andExpect(jsonPath("$.bloques[2].tipo").value("RECREO"))
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(0));
  }

  @Test
  void rechazaSegundoHorarioParaLaMismaSeccionYAnio() throws Exception {
    Escenario e = escenario();
    crearHorario(e).andExpect(status().isCreated());
    crearHorario(e).andExpect(status().isConflict());
  }

  @Test
  void rechazaSeccionInexistente() throws Exception {
    Escenario e = escenario();
    mvc.perform(
            post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cuerpo(
                        Map.of("seccionId", 999999, "anio", ANIO, "plantillaId", e.plantillaId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void rechazaPlantillaInactiva() throws Exception {
    Escenario e = escenario();
    PlantillaHorario plantilla = plantillas.findById(e.plantillaId()).orElseThrow();
    plantilla.setActive(false);
    plantillas.save(plantilla);
    crearHorario(e).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void consultaHorarioPorSeccionYAnio() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    mvc.perform(
            get(BASE).param("seccionId", String.valueOf(e.seccionId())).param("anio", "" + ANIO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(horarioId));
    mvc.perform(get(BASE).param("seccionId", String.valueOf(e.seccionId())).param("anio", "2099"))
        .andExpect(status().isNotFound());
  }

  @Test
  void asignaSesionYApareceEnLaGrilla() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.dia").value("MONDAY"))
        .andExpect(jsonPath("$.areaCodigo").value(e.areaCodigo()))
        .andExpect(jsonPath("$.aulaId").value(e.aulaId()));

    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(1))
        .andExpect(jsonPath("$.bloques[0].sesiones[0].dia").value("MONDAY"))
        .andExpect(jsonPath("$.bloques[0].sesiones[0].docenteNombre").value("Marta Quispe"))
        .andExpect(jsonPath("$.bloques[1].sesiones.length()").value(0));
  }

  @Test
  void rechazaCeldaYaOcupada() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "TUESDAY").andExpect(status().isCreated());
    agregarSesion(horarioId, e, e.bloque1Id(), "TUESDAY").andExpect(status().isConflict());
  }

  @Test
  void rechazaSesionEnRecreo() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.recreoId(), "MONDAY").andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaSabadoYDomingo() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "SATURDAY")
        .andExpect(status().isUnprocessableEntity());
    agregarSesion(horarioId, e, e.bloque1Id(), "SUNDAY")
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaDiaInexistente() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "FUNDAY").andExpect(status().isBadRequest());
  }

  @Test
  void rechazaBloqueDeOtraPlantilla() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    JsonNode otra = crearPlantilla(UUID.randomUUID().toString().substring(0, 8));
    int bloqueAjeno = otra.get("bloques").get(0).get("id").asInt();
    agregarSesion(horarioId, e, bloqueAjeno, "MONDAY").andExpect(status().isUnprocessableEntity());
  }

  @Test
  void agregarSesion_conIdDelRegistroDocente_loDevuelveEnLaRespuesta() throws Exception {
    Escenario e = escenario();
    // Si coincidieran, la prueba no distinguiria el id del docente del id de su cuenta.
    assertThat(e.docenteId()).isNotEqualTo(e.cuentaDocenteId());
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.docenteId").value(e.docenteId()));
  }

  @Test
  void miHorario_sesionAsignadaAlDocente_apareceEnSuHorario() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());
    mvc.perform(
            get("/api/v1/docentes/mi-horario")
                .param("anio", String.valueOf(ANIO))
                .with(user("docente-" + e.sufijo()).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].areaCodigo").value(e.areaCodigo()));
  }

  @Test
  void rechazaDocenteQueNoTieneRolDocente() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    Persona persona = personas.save(new Persona("3" + e.sufijo(), "Luis", "Rojas", "Luna"));
    cuentas.save(new CuentaUsuario("alumno-" + e.sufijo(), "hash", Rol.ALUMNO, persona));
    Docente perfil = new Docente();
    perfil.setPersona(persona);
    perfil = docentes.save(perfil);
    mvc.perform(
            post(BASE + "/" + horarioId + "/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesion(e, e.bloque1Id(), "MONDAY", perfil.getId(), e.aulaId())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaDocenteConCuentaDesactivada() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    CuentaUsuario cuenta = cuentas.findById(e.cuentaDocenteId()).orElseThrow();
    cuenta.setActive(false);
    cuentas.save(cuenta);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY")
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaDocenteInactivo() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    Docente perfil = docentes.findById(e.docenteId()).orElseThrow();
    perfil.setActive(false);
    docentes.save(perfil);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY")
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void rechazaAulaInactiva() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    Aula aula = aulas.findById(e.aulaId()).orElseThrow();
    aula.setActive(false);
    aulas.save(aula);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY")
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void actualizaElDiaDeUnaSesion() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    mvc.perform(
            put(BASE + "/" + horarioId + "/sesiones/" + sesionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesion(e, e.bloque1Id(), "WEDNESDAY", e.docenteId(), e.aulaId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dia").value("WEDNESDAY"));
  }

  @Test
  void actualizaSesionConVigenciaTemporalYPersisteElRango() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    LocalDate desde = LocalDate.of(ANIO, 1, 2);
    LocalDate hasta = LocalDate.of(ANIO, 1, 31);

    mvc.perform(
            put(BASE + "/" + horarioId + "/sesiones/" + sesionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    cuerpo(
                        Map.of(
                            "bloqueId",
                            e.bloque1Id(),
                            "dia",
                            "MONDAY",
                            "areaCodigo",
                            e.areaCodigo(),
                            "docenteId",
                            e.docenteId(),
                            "aulaId",
                            e.aulaId(),
                            "tipoVigencia",
                            "TEMPORAL",
                            "vigenteDesde",
                            desde.toString(),
                            "vigenteHasta",
                            hasta.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vigenteDesde").value(desde.toString()))
        .andExpect(jsonPath("$.vigenteHasta").value(hasta.toString()));

    assertThat(sesionesHorario.findAllByHorarioId(horarioId))
        .anyMatch(
            sesion ->
                desde.equals(sesion.getVigenteDesde()) && hasta.equals(sesion.getVigenteHasta()));
  }

  @Test
  void eliminaUnaSesion() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque2Id(), "FRIDAY"));
    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNoContent());
    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bloques[1].sesiones.length()").value(0));
    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNotFound());
  }

  @Test
  void horarioEstudiante_muestraSoloLaSeccionConMatriculaConfirmada() throws Exception {
    Escenario propia = escenario();
    int horarioPropio = idDe(crearHorario(propia).andExpect(status().isCreated()));
    agregarSesion(horarioPropio, propia, propia.bloque1Id(), "MONDAY")
        .andExpect(status().isCreated());

    Escenario ajena = escenario();
    int horarioAjeno = idDe(crearHorario(ajena).andExpect(status().isCreated()));
    agregarSesion(horarioAjeno, ajena, ajena.bloque1Id(), "TUESDAY")
        .andExpect(status().isCreated());

    String username = matricularAlumno(propia, EstadoMatricula.CONFIRMADA);
    mvc.perform(
            get("/api/v1/estudiantes/mi-horario")
                .param("anio", String.valueOf(ANIO))
                .with(user(username).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].areaCodigo").value(propia.areaCodigo()))
        .andExpect(jsonPath("$[0].seccionCodigo").value("T" + propia.sufijo()));
  }

  @Test
  void horarioEstudiante_noMuestraClasesConMatriculaAunNoConfirmada() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());
    String username = matricularAlumno(e, EstadoMatricula.EN_REGISTRO);

    mvc.perform(
            get("/api/v1/estudiantes/mi-horario")
                .param("anio", String.valueOf(ANIO))
                .with(user(username).roles("ALUMNO")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void estudianteNoPuedeConsultarElHorarioConsolidadoDocente() throws Exception {
    Escenario e = escenario();
    mvc.perform(
            get("/api/v1/docentes/mi-horario")
                .param("anio", String.valueOf(ANIO))
                .with(user("docente-" + e.sufijo()).roles("ALUMNO")))
        .andExpect(status().isForbidden());
  }

  @Test
  void docenteNoPuedeConsultarElHorarioConsolidadoEstudiante() throws Exception {
    Escenario e = escenario();
    mvc.perform(
            get("/api/v1/estudiantes/mi-horario")
                .param("anio", String.valueOf(ANIO))
                .with(user("docente-" + e.sufijo()).roles("DOCENTE")))
        .andExpect(status().isForbidden());
  }

  @Test
  void docenteNoPuedeAgregarEnUnBloqueYaOcupadoDeSuSeccion() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());

    mvc.perform(
            post("/api/v1/docentes/mi-horario/" + horarioId + "/sesiones")
                .with(user("docente-" + e.sufijo()).roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesionDocente(e.bloque1Id(), "MONDAY", e.areaCodigo(), e.aulaId())))
        .andExpect(status().isConflict());
  }

  @Test
  void docenteNoPuedeModificarSesionParaOcuparUnBloqueYaAsignado() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int primera = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    agregarSesion(horarioId, e, e.bloque2Id(), "WEDNESDAY").andExpect(status().isCreated());

    mvc.perform(
            put("/api/v1/docentes/mi-horario/sesiones/" + primera)
                .with(user("docente-" + e.sufijo()).roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesionDocente(e.bloque2Id(), "WEDNESDAY", e.areaCodigo(), e.aulaId())))
        .andExpect(status().isConflict());
  }

  @Test
  void docentePuedeAgregarUnaSesionPropiaDesdeSuHorarioConsolidado() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/docentes/mi-horario/" + horarioId + "/sesiones")
                .with(user("docente-" + e.sufijo()).roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesionDocente(e.bloque2Id(), "MONDAY", e.areaCodigo(), e.aulaId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.docenteId").value(e.docenteId()));
    mvc.perform(
            get("/api/v1/docentes/mi-horario")
                .param("anio", String.valueOf(ANIO))
                .with(user("docente-" + e.sufijo()).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void docenteNoPuedeAgregarSesionesEnUnaSeccionAjena() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    mvc.perform(
            post("/api/v1/docentes/mi-horario/" + horarioId + "/sesiones")
                .with(user("docente-" + e.sufijo()).roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesionDocente(e.bloque1Id(), "MONDAY", e.areaCodigo(), e.aulaId())))
        .andExpect(status().isForbidden());
  }

  @Test
  void docentePuedeModificarUnaSesionPropiaDesdeSuHorarioConsolidado() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    mvc.perform(
            put("/api/v1/docentes/mi-horario/sesiones/" + sesionId)
                .with(user("docente-" + e.sufijo()).roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesionDocente(e.bloque1Id(), "WEDNESDAY", e.areaCodigo(), e.aulaId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dia").value("WEDNESDAY"));
  }

  @Test
  void docentePuedeEliminarUnaSesionPropiaDesdeSuHorarioConsolidado() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    mvc.perform(
            delete("/api/v1/docentes/mi-horario/sesiones/" + sesionId)
                .with(user("docente-" + e.sufijo()).roles("DOCENTE")))
        .andExpect(status().isNoContent());
    mvc.perform(
            get("/api/v1/docentes/mi-horario")
                .param("anio", String.valueOf(ANIO))
                .with(user("docente-" + e.sufijo()).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void docenteNoPuedeEliminarSesionAsignadaAOtroDocente() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    String otraCuenta = "otro-docente-" + e.sufijo();
    Persona otraPersona = personas.save(new Persona("8" + e.sufijo(), "Ana", "Rios", "Paz"));
    cuentas.save(new CuentaUsuario(otraCuenta, "hash", Rol.DOCENTE, otraPersona));
    Docente otroDocente = new Docente();
    otroDocente.setPersona(otraPersona);
    docentes.save(otroDocente);
    mvc.perform(
            delete("/api/v1/docentes/mi-horario/sesiones/" + sesionId)
                .with(user(otraCuenta).roles("DOCENTE")))
        .andExpect(status().isNotFound());
  }

  @Test
  void docenteNoPuedeModificarSesionAsignadaAOtroDocente() throws Exception {
    Escenario e = escenario();
    int horarioId = idDe(crearHorario(e).andExpect(status().isCreated()));
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    String otraCuenta = "otro-docente-edicion-" + e.sufijo();
    Persona otraPersona = personas.save(new Persona("7" + e.sufijo(), "Ana", "Rios", "Paz"));
    cuentas.save(new CuentaUsuario(otraCuenta, "hash", Rol.DOCENTE, otraPersona));
    Docente otroDocente = new Docente();
    otroDocente.setPersona(otraPersona);
    docentes.save(otroDocente);

    mvc.perform(
            put("/api/v1/docentes/mi-horario/sesiones/" + sesionId)
                .with(user(otraCuenta).roles("DOCENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesionDocente(e.bloque1Id(), "WEDNESDAY", e.areaCodigo(), e.aulaId())))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "docente-test", roles = "DOCENTE")
  void rechazaUsuariosSinRolAdministrador() throws Exception {
    mvc.perform(get(BASE + "/1")).andExpect(status().isForbidden());
  }

  // ---------- apoyo ----------

  private Escenario escenario() throws Exception {
    String s = UUID.randomUUID().toString().substring(0, 8);
    JsonNode plantilla = crearPlantilla(s);

    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(1, NivelEducativo.SECUNDARIA)
            .orElseThrow();
    Seccion seccion = new Seccion();
    seccion.setCodigo("T" + s);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    seccion = secciones.save(seccion);

    Area area = new Area();
    area.setCodigo("TST-" + s);
    area.setNombre("Área de prueba " + s);
    area = areas.save(area);

    Aula aula = new Aula();
    aula.setCodigo("T-" + s);
    aula.setCapacidad(30);
    aula = aulas.save(aula);

    // Una cuenta extra sin perfil docente desfasa los ids de cuentas y de docentes.
    Persona otraPersona = personas.save(new Persona("4" + s, "Rosa", "Huaman", "Paz"));
    cuentas.save(new CuentaUsuario("apoderado-" + s, "hash", Rol.APODERADO, otraPersona));
    Persona persona = personas.save(new Persona("2" + s, "Marta", "Quispe", "Salas"));
    CuentaUsuario cuenta =
        cuentas.save(new CuentaUsuario("docente-" + s, "hash", Rol.DOCENTE, persona));
    Docente docente = new Docente();
    docente.setPersona(persona);
    docente = docentes.save(docente);

    JsonNode bloques = plantilla.get("bloques");
    return new Escenario(
        seccion.getId(),
        plantilla.get("id").asInt(),
        bloques.get(0).get("id").asInt(),
        bloques.get(1).get("id").asInt(),
        bloques.get(2).get("id").asInt(),
        area.getCodigo(),
        docente.getId(),
        cuenta.getId(),
        aula.getId(),
        s);
  }

  private JsonNode crearPlantilla(String sufijo) throws Exception {
    String cuerpo =
        """
        {"nombre":"Plantilla %s","turno":"MANANA","horaInicio":"07:30:00","horaFin":"13:00:00",
        "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":6,
        "recreos":[{"despuesDeBloque":2,"duracionMinutos":15},{"despuesDeBloque":4,"duracionMinutos":20}]}
        """
            .formatted(sufijo);
    ResultActions creada =
        mvc.perform(
                post("/api/v1/horarios/plantillas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
            .andExpect(status().isCreated());
    return json.readTree(creada.andReturn().getResponse().getContentAsString());
  }

  private ResultActions crearHorario(Escenario e) throws Exception {
    return mvc.perform(
        post(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                cuerpo(
                    Map.of(
                        "seccionId",
                        e.seccionId(),
                        "anio",
                        ANIO,
                        "plantillaId",
                        e.plantillaId()))));
  }

  private ResultActions agregarSesion(int horarioId, Escenario e, int bloqueId, String dia)
      throws Exception {
    return mvc.perform(
        post(BASE + "/" + horarioId + "/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(sesion(e, bloqueId, dia, e.docenteId(), e.aulaId())));
  }

  private String sesion(Escenario e, int bloqueId, String dia, int docenteId, int aulaId)
      throws Exception {
    return cuerpo(
        Map.of(
            "bloqueId", bloqueId,
            "dia", dia,
            "areaCodigo", e.areaCodigo(),
            "docenteId", docenteId,
            "aulaId", aulaId));
  }

  private String sesionDocente(int bloqueId, String dia, String areaCodigo, int aulaId)
      throws Exception {
    return cuerpo(
        Map.of("bloqueId", bloqueId, "dia", dia, "areaCodigo", areaCodigo, "aulaId", aulaId));
  }

  private String matricularAlumno(Escenario e, EstadoMatricula estado) {
    Seccion seccion = secciones.findById(e.seccionId()).orElseThrow();
    Persona persona = personas.save(new Persona("9" + e.sufijo(), "Alumno", "Prueba", e.sufijo()));
    CuentaUsuario cuenta =
        cuentas.save(new CuentaUsuario("alumno-" + e.sufijo(), "hash", Rol.ALUMNO, persona));
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    estudiantes.save(estudiante);

    Periodo periodo = new Periodo();
    periodo.setGrado(seccion.getGrado());
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(LocalDate.of(ANIO, 3, 1));
    periodo.setFechaFin(LocalDate.of(ANIO, 12, 20));
    periodo.setEstado(EstadoPeriodo.PLANIFICADO);
    periodos.save(periodo);

    Matricula matricula = new Matricula();
    matricula.setFechaMatricula(LocalDate.of(ANIO, 2, 1));
    matricula.setEstado(estado);
    matricula.setPeriodo(periodo);
    matricula.setEstudiante(estudiante);
    matriculas.save(matricula);
    return cuenta.getUsername();
  }

  private String cuerpo(Object datos) throws Exception {
    return json.writeValueAsString(datos);
  }

  private int idDe(ResultActions respuesta) throws Exception {
    String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
    return json.readTree(cuerpo).get("id").asInt();
  }
}
