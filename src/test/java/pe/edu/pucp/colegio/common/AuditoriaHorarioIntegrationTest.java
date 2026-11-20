package pe.edu.pucp.colegio.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.AuditoriaHorario;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AuditoriaHorarioRepository;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/**
 * Auditoría de cambios de horario (US-4.5) a través del mismo endpoint que consume el frontend
 * ({@code GET /api/v1/horarios/auditoria}), con el reloj fijo en el miércoles 18/11/2026 10:00.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "auditoria-horario-test", roles = "ADMINISTRADOR")
@Transactional
class AuditoriaHorarioIntegrationTest {

  private static final ZoneId LIMA = ZoneId.of("America/Lima");
  private static final LocalDate HOY = LocalDate.of(2026, 11, 18);
  private static final int ANIO_ACTUAL = 2026;
  private static final int ANIO_SIGUIENTE = 2027;
  private static final String BASE = "/api/v1/horarios/secciones";
  private static final String AUDITORIA = "/api/v1/horarios/auditoria";

  @TestConfiguration
  static class RelojFijoConfig {
    @Bean
    @Primary
    Clock relojAuditoriaHorario() {
      return Clock.fixed(LocalDateTime.of(HOY, LocalTime.of(10, 0)).atZone(LIMA).toInstant(), LIMA);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @PersistenceContext EntityManager entityManager;
  @Autowired AuditoriaHorarioRepository auditoria;
  @Autowired GradoRepository grados;
  @Autowired SeccionRepository secciones;
  @Autowired AreaRepository areas;
  @Autowired AulaRepository aulas;
  @Autowired PersonaRepository personas;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired DocenteRepository docentes;

  /** Sección, plantilla, área, dos docentes y dos aulas listos para armar un horario. */
  record Escenario(
      int seccionId,
      int plantillaId,
      int bloque1Id,
      int bloque2Id,
      String areaCodigo,
      int docenteId,
      int docente2Id,
      int aulaId,
      int aula2Id,
      String aulaCodigo,
      String aula2Codigo) {}

  @Test
  void agregarSesion_registraCreacionConElContratoDelFrontend() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());

    consultar(horarioId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].horarioId").value(horarioId))
        .andExpect(jsonPath("$[0].accion").value("CREACIÓN"))
        .andExpect(jsonPath("$[0].usuarioNombre").value("auditoria-horario-test"))
        .andExpect(jsonPath("$[0].usuarioCuentaId").isEmpty())
        .andExpect(jsonPath("$[0].valoresAnteriores").isEmpty())
        .andExpect(jsonPath("$[0].vigenciaTipo").value("PERMANENTE"))
        .andExpect(jsonPath("$[0].vigenteDesde").value("2027-01-01"));
  }

  @Test
  void agregarSesion_detalleConDiaEnEspaniol() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());

    String detalle = registros(horarioId).get(0).getDetalle();
    assertThat(detalle).startsWith("Día lunes, bloque 07:30–08:15");
    assertThat(detalle).doesNotContain("MONDAY");
  }

  @Test
  void actualizarSesion_enElLugar_guardaElEstadoAnteriorReal() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));

    actualizarSesion(
            horarioId, sesionId, sesion(e, e.bloque1Id(), "MONDAY", e.docenteId(), e.aula2Id()))
        .andExpect(status().isOk());

    AuditoriaHorario modificacion = registros(horarioId).get(0);
    assertThat(modificacion.getAccion()).isEqualTo("ACTUALIZACIÓN");
    assertThat(modificacion.getValoresAnteriores()).contains("aula " + e.aulaCodigo());
    assertThat(modificacion.getValoresNuevos()).contains("aula " + e.aula2Codigo());
  }

  @Test
  void actualizarSesion_conCierreDeVigencia_rigeDesdeManiana() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_ACTUAL);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));

    actualizarSesion(
            horarioId, sesionId, sesion(e, e.bloque1Id(), "MONDAY", e.docenteId(), e.aula2Id()))
        .andExpect(status().isOk());

    List<AuditoriaHorario> registros = registros(horarioId);
    assertThat(registros)
        .extracting(AuditoriaHorario::getAccion)
        .containsExactly("ACTUALIZACIÓN", "CREACIÓN");
    assertThat(registros.get(0).getVigenteDesde()).isEqualTo(HOY.plusDays(1));
  }

  @Test
  void actualizarSesion_sinCambios_noRegistraModificacion() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));

    actualizarSesion(
            horarioId, sesionId, sesion(e, e.bloque1Id(), "MONDAY", e.docenteId(), e.aulaId()))
        .andExpect(status().isOk());

    assertThat(registros(horarioId))
        .extracting(AuditoriaHorario::getAccion)
        .containsExactly("CREACIÓN");
  }

  @Test
  void actualizarSesion_conCruce_noRegistraModificacion() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());
    int segunda = idDe(agregarSesion(horarioId, e, e.bloque2Id(), "MONDAY"));

    actualizarSesion(
            horarioId, segunda, sesion(e, e.bloque1Id(), "MONDAY", e.docenteId(), e.aulaId()))
        .andExpect(status().isConflict());

    assertThat(registros(horarioId))
        .extracting(AuditoriaHorario::getAccion)
        .containsExactly("CREACIÓN", "CREACIÓN");
  }

  @Test
  void actualizarSesion_temporal_reasignaSoloElRangoYRegistraLaVigencia() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_ACTUAL);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    SesionClase dentroDelRango = generarClase(sesionId, LocalDate.of(2026, 11, 23));
    SesionClase despuesDelRango = generarClase(sesionId, LocalDate.of(2026, 11, 30));
    Map<String, Object> temporal = sesion(e, e.bloque1Id(), "MONDAY", e.docente2Id(), e.aulaId());
    temporal.put("tipoVigencia", "TEMPORAL");
    temporal.put("vigenteDesde", "2026-11-23");
    temporal.put("vigenteHasta", "2026-11-27");

    actualizarSesion(horarioId, sesionId, temporal).andExpect(status().isOk());
    entityManager.flush();
    entityManager.clear();

    assertThat(entityManager.find(SesionClase.class, dentroDelRango.getId()).getDocente().getId())
        .isEqualTo(e.docente2Id());
    assertThat(entityManager.find(SesionClase.class, despuesDelRango.getId()).getDocente().getId())
        .isEqualTo(e.docenteId());
    AuditoriaHorario modificacion = registros(horarioId).get(0);
    assertThat(modificacion.getAccion()).isEqualTo("ACTUALIZACIÓN");
    assertThat(modificacion.getVigenciaTipo()).isEqualTo("TEMPORAL");
    assertThat(modificacion.getVigenteDesde()).isEqualTo(LocalDate.of(2026, 11, 23));
    assertThat(modificacion.getVigenteHasta()).isEqualTo(LocalDate.of(2026, 11, 27));
  }

  @Test
  void actualizarSesion_temporalConRangoInvalido_responde422SinRegistrar() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_ACTUAL);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    Map<String, Object> temporal = sesion(e, e.bloque1Id(), "MONDAY", e.docente2Id(), e.aulaId());
    temporal.put("tipoVigencia", "TEMPORAL");
    temporal.put("vigenteDesde", "2026-11-27");
    temporal.put("vigenteHasta", "2026-11-23");

    actualizarSesion(horarioId, sesionId, temporal).andExpect(status().isUnprocessableEntity());

    assertThat(registros(horarioId))
        .extracting(AuditoriaHorario::getAccion)
        .containsExactly("CREACIÓN");
  }

  @Test
  void eliminarSesion_conClasesGeneradas_rigeDesdeManiana() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_ACTUAL);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    generarClase(sesionId, LocalDate.of(2026, 11, 23));

    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNoContent());

    AuditoriaHorario retiro = registros(horarioId).get(0);
    assertThat(retiro.getAccion()).isEqualTo("ELIMINACIÓN");
    assertThat(retiro.getValoresNuevos()).isNull();
    assertThat(retiro.getVigenteDesde()).isEqualTo(HOY.plusDays(1));
  }

  @Test
  void eliminarSesion_futuraConClases_rigeDesdeElInicioDeSuVigencia() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));
    generarClase(sesionId, LocalDate.of(2027, 1, 4));

    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNoContent());

    assertThat(registros(horarioId).get(0).getVigenteDesde())
        .isEqualTo(LocalDate.of(ANIO_SIGUIENTE, 1, 1));
  }

  @Test
  void eliminarSesion_sinClases_registraRetiroInmediato() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    int sesionId = idDe(agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY"));

    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNoContent());

    AuditoriaHorario retiro = registros(horarioId).get(0);
    assertThat(retiro.getAccion()).isEqualTo("ELIMINACIÓN");
    assertThat(retiro.getVigenteDesde()).isEqualTo(HOY);
  }

  @Test
  void listar_porHorario_soloDevuelveEseHorario() throws Exception {
    Escenario e = escenario();
    Escenario otro = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    int otroHorarioId = crearHorario(otro, ANIO_SIGUIENTE);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());
    agregarSesion(otroHorarioId, otro, otro.bloque1Id(), "MONDAY").andExpect(status().isCreated());

    consultar(horarioId)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].horarioId").value(horarioId));
  }

  @Test
  void listar_sinFechas_usaElRangoPorDefectoDelRelojDelSistema() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    agregarSesion(horarioId, e, e.bloque1Id(), "MONDAY").andExpect(status().isCreated());

    mvc.perform(get(AUDITORIA).param("horarioId", String.valueOf(horarioId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void listar_desdePosteriorAHasta_responde422() throws Exception {
    mvc.perform(get(AUDITORIA).param("desde", "2026-11-20").param("hasta", "2026-11-19"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422));
  }

  @Test
  @WithMockUser(username = "docente-auditoria", roles = "DOCENTE")
  void listar_comoDocente_responde403() throws Exception {
    mvc.perform(get(AUDITORIA)).andExpect(status().isForbidden());
  }

  @Test
  void guardar_accionFueraDelCatalogo_laRechazaLaBaseDeDatos() throws Exception {
    Escenario e = escenario();
    int horarioId = crearHorario(e, ANIO_SIGUIENTE);
    AuditoriaHorario registro = registroValido(horarioId);
    registro.setAccion("BORRADO");

    assertThatThrownBy(() -> auditoria.saveAndFlush(registro))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void guardar_horarioInexistente_loRechazaLaBaseDeDatos() {
    AuditoriaHorario registro = registroValido(999_999);

    assertThatThrownBy(() -> auditoria.saveAndFlush(registro))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // ---------- apoyo ----------

  private AuditoriaHorario registroValido(int horarioId) {
    AuditoriaHorario registro = new AuditoriaHorario();
    registro.setHorarioId(horarioId);
    registro.setSeccion("1° Prueba");
    registro.setAccion("CREACIÓN");
    registro.setFechaHora(LocalDateTime.of(HOY, LocalTime.NOON));
    registro.setUsuarioNombre("prueba");
    registro.setDetalle("Detalle de prueba");
    registro.setVigenciaTipo("PERMANENTE");
    return registro;
  }

  private ResultActions consultar(int horarioId) throws Exception {
    return mvc.perform(
        get(AUDITORIA)
            .param("horarioId", String.valueOf(horarioId))
            .param("desde", "2026-01-01")
            .param("hasta", "2026-12-31"));
  }

  /** Del más reciente al más antiguo; a igual hora, el último insertado primero. */
  private List<AuditoriaHorario> registros(int horarioId) {
    entityManager.flush();
    return auditoria
        .findAllByHorarioIdAndFechaHoraBetweenOrderByFechaHoraDesc(
            horarioId, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 12, 31, 0, 0))
        .stream()
        .sorted((a, b) -> b.getId() - a.getId())
        .toList();
  }

  private SesionClase generarClase(int sesionHorarioId, LocalDate fecha) {
    SesionHorario sesionHorario = entityManager.find(SesionHorario.class, sesionHorarioId);
    SesionClase clase = new SesionClase();
    clase.setSeccion(sesionHorario.getHorario().getSeccion());
    clase.setArea(sesionHorario.getArea());
    clase.setDocente(sesionHorario.getDocente());
    clase.setSesionHorario(sesionHorario);
    clase.setFecha(fecha);
    clase.setHoraInicio(sesionHorario.getBloque().getHoraInicio());
    clase.setHoraFin(sesionHorario.getBloque().getHoraFin());
    entityManager.persist(clase);
    entityManager.flush();
    return clase;
  }

  private Escenario escenario() throws Exception {
    String s = UUID.randomUUID().toString().substring(0, 8);
    JsonNode plantilla = crearPlantilla(s);

    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(1, NivelEducativo.SECUNDARIA)
            .orElseThrow();
    Seccion seccion = new Seccion();
    seccion.setCodigo("H" + s);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    seccion = secciones.save(seccion);

    Area area = new Area();
    area.setCodigo("AH-" + s);
    area.setNombre("Área auditoría " + s);
    area = areas.save(area);

    Aula aula = aulas.save(nuevaAula("A1-" + s));
    Aula aula2 = aulas.save(nuevaAula("A2-" + s));

    JsonNode bloques = plantilla.get("bloques");
    return new Escenario(
        seccion.getId(),
        plantilla.get("id").asInt(),
        bloques.get(0).get("id").asInt(),
        bloques.get(1).get("id").asInt(),
        area.getCodigo(),
        nuevoDocente("5" + s, "Marta"),
        nuevoDocente("6" + s, "Luis"),
        aula.getId(),
        aula2.getId(),
        aula.getCodigo(),
        aula2.getCodigo());
  }

  private int nuevoDocente(String dni, String nombre) {
    Persona persona = personas.save(new Persona(dni, nombre, "Quispe", "Salas"));
    cuentas.save(new CuentaUsuario("aud-hor-" + dni, "hash", Rol.DOCENTE, persona));
    Docente docente = new Docente();
    docente.setPersona(persona);
    return docentes.save(docente).getId();
  }

  private Aula nuevaAula(String codigo) {
    Aula aula = new Aula();
    aula.setCodigo(codigo);
    aula.setCapacidad(30);
    return aula;
  }

  private JsonNode crearPlantilla(String sufijo) throws Exception {
    String cuerpo =
        """
        {"nombre":"Plantilla auditoría %s","turno":"MANANA","horaInicio":"07:30:00",
        "horaFin":"13:00:00","duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":6,
        "recreos":[{"despuesDeBloque":2,"duracionMinutos":15}]}
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

  private int crearHorario(Escenario e, int anio) throws Exception {
    return idDe(
        mvc.perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "seccionId", e.seccionId(),
                                "anio", anio,
                                "plantillaId", e.plantillaId()))))
            .andExpect(status().isCreated()));
  }

  private ResultActions agregarSesion(int horarioId, Escenario e, int bloqueId, String dia)
      throws Exception {
    return mvc.perform(
        post(BASE + "/" + horarioId + "/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(sesion(e, bloqueId, dia, e.docenteId(), e.aulaId()))));
  }

  private ResultActions actualizarSesion(int horarioId, int sesionId, Map<String, Object> cuerpo)
      throws Exception {
    return mvc.perform(
        put(BASE + "/" + horarioId + "/sesiones/" + sesionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(cuerpo)));
  }

  private Map<String, Object> sesion(
      Escenario e, int bloqueId, String dia, int docenteId, int aulaId) {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("bloqueId", bloqueId);
    cuerpo.put("dia", dia);
    cuerpo.put("areaCodigo", e.areaCodigo());
    cuerpo.put("docenteId", docenteId);
    cuerpo.put("aulaId", aulaId);
    return cuerpo;
  }

  private int idDe(ResultActions respuesta) throws Exception {
    String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
    return json.readTree(cuerpo).get("id").asInt();
  }
}
