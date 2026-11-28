package pe.edu.pucp.colegio.common;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.config.RelojSistema;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/**
 * Quitar o editar una clase del año en curso se ve en la grilla el mismo día, salvo que ya empezó.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "horario-vigencia-test", roles = "ADMINISTRADOR")
@Transactional
class HorarioSeccionVigenciaIntegrationTest {

  private static final String BASE = "/api/v1/horarios/secciones";
  private static final int ANIO = 2031;
  private static final LocalDate LUNES = LocalDate.of(ANIO, 6, 9);
  private static final LocalDate MIERCOLES = LocalDate.of(ANIO, 6, 11);

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @PersistenceContext EntityManager entityManager;
  @Autowired RelojSistema reloj;
  @Autowired GradoRepository grados;
  @Autowired SeccionRepository secciones;
  @Autowired PeriodoRepository periodos;
  @Autowired AreaRepository areas;
  @Autowired AulaRepository aulas;
  @Autowired PersonaRepository personas;
  @Autowired CuentaUsuarioRepository cuentas;
  @Autowired DocenteRepository docentes;

  private int seccionId;
  private int bloque1;
  private int bloque2;
  private String areaCodigo;
  private int docenteId;
  private int aulaId;

  @AfterEach
  void volverAlRelojReal() {
    reloj.ajustar(Duration.ZERO);
  }

  @Test
  void quitarAntesDeLaClaseDeHoyLaSacaDeLaGrillaAlInstante() throws Exception {
    int horarioId = horarioConClaseDelMiercoles(MIERCOLES, LocalTime.of(6, 0));
    int sesionId = primeraSesion(horarioId);
    assertThat(clasesDeLaSeccion()).isPositive();

    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNoContent());

    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(0));
    assertThat(clasesDeLaSeccion()).isZero();
  }

  @Test
  void quitarDespuesDeEmpezarLaClaseDeHoyLaConservaSoloHoy() throws Exception {
    int horarioId = horarioConClaseDelMiercoles(MIERCOLES, LocalTime.of(6, 0));
    int sesionId = primeraSesion(horarioId);
    fijarReloj(MIERCOLES, LocalTime.of(10, 0));

    mvc.perform(delete(BASE + "/" + horarioId + "/sesiones/" + sesionId))
        .andExpect(status().isNoContent());

    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(1))
        .andExpect(jsonPath("$.bloques[0].sesiones[0].vigenteHasta").value(MIERCOLES.toString()));
    assertThat(cancelada(MIERCOLES)).isFalse();
    assertThat(cancelada(MIERCOLES.plusWeeks(1))).isTrue();
  }

  @Test
  void editarAntesDeLaClaseDeHoyCambiaLaGrillaYLaClaseDeHoy() throws Exception {
    int horarioId = horarioConClaseDelMiercoles(MIERCOLES, LocalTime.of(6, 0));
    int sesionId = primeraSesion(horarioId);

    mvc.perform(
            put(BASE + "/" + horarioId + "/sesiones/" + sesionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesion(bloque2, "WEDNESDAY")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(sesionId))
        .andExpect(jsonPath("$.vigenteDesde").value(MIERCOLES.toString()));

    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(0))
        .andExpect(jsonPath("$.bloques[1].sesiones.length()").value(1));
    entityManager.flush();
    assertThat(
            jdbc.queryForObject(
                "select hora_inicio from sesiones_clase where seccion_id=? and fecha=? and cancelada=false",
                LocalTime.class,
                seccionId,
                MIERCOLES))
        .isEqualTo(LocalTime.of(8, 15));
  }

  @Test
  void editarUnaClaseQueYaSeDictoRigeDesdeHoySiLaDeHoyNoEmpezo() throws Exception {
    int horarioId = horarioConClaseDelMiercoles(LUNES, LocalTime.of(6, 0));
    int sesionId = primeraSesion(horarioId);
    fijarReloj(MIERCOLES, LocalTime.of(6, 0));

    mvc.perform(
            put(BASE + "/" + horarioId + "/sesiones/" + sesionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesion(bloque2, "WEDNESDAY")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vigenteDesde").value(MIERCOLES.toString()));

    mvc.perform(get(BASE + "/" + horarioId))
        .andExpect(jsonPath("$.bloques[0].sesiones.length()").value(0))
        .andExpect(jsonPath("$.bloques[1].sesiones.length()").value(1));
  }

  private int horarioConClaseDelMiercoles(LocalDate dia, LocalTime hora) throws Exception {
    fijarReloj(dia, hora);
    String s = UUID.randomUUID().toString().substring(0, 8);
    JsonNode plantilla = crearPlantilla(s);
    bloque1 = plantilla.get("bloques").get(0).get("id").asInt();
    bloque2 = plantilla.get("bloques").get(1).get("id").asInt();

    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(1, NivelEducativo.SECUNDARIA)
            .orElseThrow();
    Seccion seccion = new Seccion();
    seccion.setCodigo("V" + s);
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    seccion = secciones.save(seccion);
    seccionId = seccion.getId();

    Periodo periodo = new Periodo();
    periodo.setGrado(grado);
    periodo.setSeccion(seccion);
    periodo.setFechaInicio(LocalDate.of(ANIO, 3, 1));
    periodo.setFechaFin(LocalDate.of(ANIO, 12, 20));
    periodo.setEstado(EstadoPeriodo.EN_CURSO);
    periodos.save(periodo);

    Area area = new Area();
    area.setCodigo("VIG-" + s);
    area.setNombre("Área de vigencia " + s);
    areaCodigo = areas.save(area).getCodigo();

    Aula aula = new Aula();
    aula.setCodigo("V-" + s);
    aula.setCapacidad(30);
    aulaId = aulas.save(aula).getId();

    Persona persona = personas.save(new Persona("5" + s, "Elena", "Torres", "Ríos"));
    cuentas.save(new CuentaUsuario("docente-vig-" + s, "hash", Rol.DOCENTE, persona));
    Docente docente = new Docente();
    docente.setPersona(persona);
    docenteId = docentes.save(docente).getId();

    ResultActions horario =
        mvc.perform(
                post(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json.writeValueAsString(
                            Map.of(
                                "seccionId",
                                seccionId,
                                "anio",
                                ANIO,
                                "plantillaId",
                                plantilla.get("id").asInt()))))
            .andExpect(status().isCreated());
    int horarioId = idDe(horario);
    mvc.perform(
            post(BASE + "/" + horarioId + "/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sesion(bloque1, "WEDNESDAY")))
        .andExpect(status().isCreated());
    return horarioId;
  }

  private JsonNode crearPlantilla(String sufijo) throws Exception {
    String cuerpo =
        """
        {"nombre":"Vigencia %s","turno":"MANANA","horaInicio":"07:30:00","horaFin":"13:00:00",
        "duracionHoraPedagogicaMinutos":45,"cantidadBloquesPedagogicos":6,
        "recreos":[{"despuesDeBloque":2,"duracionMinutos":15},{"despuesDeBloque":4,"duracionMinutos":20}]}
        """
            .formatted(sufijo);
    return json.readTree(
        mvc.perform(
                post("/api/v1/horarios/plantillas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private String sesion(int bloqueId, String dia) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "bloqueId",
            bloqueId,
            "dia",
            dia,
            "areaCodigo",
            areaCodigo,
            "docenteId",
            docenteId,
            "aulaId",
            aulaId));
  }

  private int primeraSesion(int horarioId) throws Exception {
    String cuerpo =
        mvc.perform(get(BASE + "/" + horarioId)).andReturn().getResponse().getContentAsString();
    return json.readTree(cuerpo).get("bloques").get(0).get("sesiones").get(0).get("id").asInt();
  }

  private int clasesDeLaSeccion() {
    entityManager.flush();
    return jdbc.queryForObject(
        "select count(*) from sesiones_clase where seccion_id=?", Integer.class, seccionId);
  }

  private boolean cancelada(LocalDate fecha) {
    entityManager.flush();
    return jdbc.queryForObject(
        "select cancelada from sesiones_clase where seccion_id=? and fecha=?",
        Boolean.class,
        seccionId,
        fecha);
  }

  private void fijarReloj(LocalDate dia, LocalTime hora) {
    Instant objetivo = LocalDateTime.of(dia, hora).atZone(reloj.getZone()).toInstant();
    reloj.ajustar(Duration.between(reloj.real().instant(), objetivo));
  }

  private int idDe(ResultActions respuesta) throws Exception {
    return json.readTree(respuesta.andReturn().getResponse().getContentAsString())
        .get("id")
        .asInt();
  }
}
