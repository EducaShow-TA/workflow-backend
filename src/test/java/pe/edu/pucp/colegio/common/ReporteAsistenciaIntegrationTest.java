package pe.edu.pucp.colegio.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.fixture.HorarioFixture;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.repository.ConsolidadoAsistenciaFila;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReporteAsistenciaIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(50_000);
  private static final LocalDate FECHA = LocalDate.of(2026, 11, 4);
  private static final LocalTime HORA = LocalTime.of(8, 0);

  private static final String RUTA = "/api/v1/reportes/asistencia-estudiantes";

  @Autowired MockMvc mvc;
  @Autowired AsistenciaEstudianteRepository asistencias;
  @Autowired SesionClaseRepository sesiones;
  @Autowired SeccionRepository secciones;
  @PersistenceContext EntityManager entityManager;

  // ---------- Repositorio ----------

  @Test
  void consolidarPorAlumno_variosEstados_cuentaCadaEstadoYExcluyePendiente() {
    Escenario e = escenario();
    Estudiante alumno = estudiante("Ana", "Quispe", "Rojas");
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarPresente);
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 1),
        AsistenciaEstudiante::marcarTardanza);
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 2),
        AsistenciaEstudiante::marcarTardanza);
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 3),
        AsistenciaEstudiante::marcarAusente);
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 4),
        a -> a.justificar("Cita médica", null));
    marcar(alumno, sesion(e.seccionA, e.area, e.docente, FECHA, 5), a -> {});

    List<ConsolidadoAsistenciaFila> filas =
        asistencias.consolidarPorAlumno(FECHA, FECHA, null, e.seccionA.getId(), null);

    assertThat(filas).hasSize(1);
    ConsolidadoAsistenciaFila fila = filas.get(0);
    assertThat(fila.getEstudianteId()).isEqualTo(alumno.getId());
    assertThat(fila.getNombre()).isEqualTo("Ana");
    assertThat(fila.getApellidoPaterno()).isEqualTo("Quispe");
    assertThat(fila.getGradoId()).isEqualTo(e.seccionA.getGrado().getId());
    assertThat(fila.getNivelEducativo()).isEqualTo(NivelEducativo.SECUNDARIA);
    assertThat(fila.getSeccionCodigo()).isEqualTo("A");
    assertThat(fila.getPresentes()).isEqualTo(1L);
    assertThat(fila.getTardanzas()).isEqualTo(2L);
    assertThat(fila.getInasistencias()).isEqualTo(1L);
    assertThat(fila.getJustificadas()).isEqualTo(1L);
  }

  @Test
  void consolidarPorAlumno_fueraDeRangoInactivoOCancelada_noCuenta() {
    Escenario e = escenario();
    Estudiante alumno = estudiante("Luis", "Rojas", "Diaz");
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarAusente);
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA.minusDays(1), 0),
        AsistenciaEstudiante::marcarAusente);
    SesionClase inactiva = sesion(e.seccionA, e.area, e.docente, FECHA, 1);
    inactiva.setActive(false);
    marcar(alumno, inactiva, AsistenciaEstudiante::marcarAusente);
    SesionClase cancelada = sesion(e.seccionA, e.area, e.docente, FECHA, 2);
    cancelada.setCancelada(true);
    marcar(alumno, cancelada, AsistenciaEstudiante::marcarAusente);
    AsistenciaEstudiante registroInactivo =
        marcar(
            alumno,
            sesion(e.seccionA, e.area, e.docente, FECHA, 3),
            AsistenciaEstudiante::marcarAusente);
    registroInactivo.setActive(false);
    entityManager.flush();

    List<ConsolidadoAsistenciaFila> filas =
        asistencias.consolidarPorAlumno(FECHA, FECHA, null, e.seccionA.getId(), null);

    assertThat(filas)
        .singleElement()
        .extracting(ConsolidadoAsistenciaFila::getInasistencias)
        .isEqualTo(1L);
  }

  @Test
  void consolidarPorAlumno_rangoDeUnDia_incluyeLimites() {
    Escenario e = escenario();
    Estudiante alumno = estudiante("Eva", "Soto", "Paz");
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarPresente);

    assertThat(asistencias.consolidarPorAlumno(FECHA, FECHA, null, e.seccionA.getId(), null))
        .singleElement()
        .extracting(ConsolidadoAsistenciaFila::getPresentes)
        .isEqualTo(1L);
    assertThat(
            asistencias.consolidarPorAlumno(
                FECHA.plusDays(1), FECHA.plusDays(2), null, e.seccionA.getId(), null))
        .isEmpty();
  }

  @Test
  void consolidarPorAlumno_alumnoCambiaDeSeccion_devuelveUnaFilaPorSeccion() {
    Escenario e = escenario();
    Estudiante alumno = estudiante("Rosa", "Lima", "Vera");
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarAusente);
    marcar(
        alumno,
        sesion(e.seccionB, e.area, e.docente, FECHA.plusDays(1), 0),
        AsistenciaEstudiante::marcarTardanza);

    List<ConsolidadoAsistenciaFila> filas =
        asistencias.consolidarPorAlumno(
            FECHA, FECHA.plusDays(1), e.seccionA.getGrado().getId(), null, null);

    assertThat(filas).hasSize(2);
    assertThat(filas)
        .extracting(ConsolidadoAsistenciaFila::getSeccionId)
        .containsExactlyInAnyOrder(e.seccionA.getId(), e.seccionB.getId());
    ConsolidadoAsistenciaFila filaA =
        filas.stream().filter(f -> f.getSeccionId().equals(e.seccionA.getId())).findFirst().get();
    assertThat(filaA.getInasistencias()).isEqualTo(1L);
    assertThat(filaA.getTardanzas()).isZero();
  }

  @Test
  void consolidarPorAlumno_filtroGradoYSeccion_restringe() {
    Escenario e = escenario();
    Estudiante alumnoA = estudiante("Juan", "Paz", "Ruiz");
    Estudiante alumnoB = estudiante("Mia", "Ruiz", "Paz");
    marcar(
        alumnoA,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarPresente);
    marcar(
        alumnoB,
        sesion(e.seccionB, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarPresente);
    Integer gradoId = e.seccionA.getGrado().getId();

    assertThat(asistencias.consolidarPorAlumno(FECHA, FECHA, gradoId, e.seccionA.getId(), null))
        .extracting(ConsolidadoAsistenciaFila::getEstudianteId)
        .containsExactly(alumnoA.getId());
    assertThat(asistencias.consolidarPorAlumno(FECHA, FECHA, gradoId, null, null))
        .extracting(ConsolidadoAsistenciaFila::getEstudianteId)
        .containsExactlyInAnyOrder(alumnoA.getId(), alumnoB.getId());
    assertThat(asistencias.consolidarPorAlumno(FECHA, FECHA, gradoId + 1, null, null))
        .extracting(ConsolidadoAsistenciaFila::getEstudianteId)
        .doesNotContain(alumnoA.getId(), alumnoB.getId());
  }

  @Test
  void consolidarPorAlumno_docente_veSuSeccionCompletaPeroNoSeccionesAjenas() {
    Escenario e = escenario();
    Docente tutor = docente("tutor");
    HorarioFixture.asignar(
        entityManager, e.seccionA, e.area, usuarioDe(tutor), FECHA, HORA, HORA.plusMinutes(45));
    Docente soloSesion = docente("suplente");
    Area otraArea = area();
    Estudiante alumnoA = estudiante("Teo", "Vega", "Luna");
    Estudiante alumnoB = estudiante("Ivy", "Luna", "Vega");
    // Otra área de la sección A, dictada por otro docente: el tutor la ve por tener clase en A.
    marcar(
        alumnoA,
        sesion(e.seccionA, otraArea, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarAusente);
    // Sección B, donde el tutor no tiene clase: no la ve.
    marcar(
        alumnoB,
        sesion(e.seccionB, e.area, soloSesion, FECHA, 0),
        AsistenciaEstudiante::marcarAusente);

    assertThat(asistencias.consolidarPorAlumno(FECHA, FECHA, null, null, personaId(tutor)))
        .extracting(ConsolidadoAsistenciaFila::getEstudianteId)
        .containsExactly(alumnoA.getId());
    // Un docente sin SesionHorario ve las sesiones que dicta.
    assertThat(asistencias.consolidarPorAlumno(FECHA, FECHA, null, null, personaId(soloSesion)))
        .extracting(ConsolidadoAsistenciaFila::getEstudianteId)
        .containsExactly(alumnoB.getId());
  }

  @Test
  void buscarSeccionesDelDocente_horarioOSesionDictada_devuelveSinDuplicados() {
    Escenario e = escenario();
    Docente tutor = docente("tutor");
    HorarioFixture.asignar(
        entityManager, e.seccionA, e.area, usuarioDe(tutor), FECHA, HORA, HORA.plusMinutes(45));
    sesion(e.seccionA, e.area, tutor, FECHA, 0);
    sesion(e.seccionB, e.area, tutor, FECHA, 1);
    entityManager.flush();

    assertThat(secciones.buscarSeccionesDelDocente(personaId(tutor)))
        .extracting(Seccion::getId)
        .containsExactlyInAnyOrder(e.seccionA.getId(), e.seccionB.getId());
    assertThat(
            sesiones.existsByDocentePersonaIdAndSeccionIdAndFechaBetweenAndActiveTrue(
                personaId(tutor), e.seccionB.getId(), FECHA, FECHA))
        .isTrue();
    assertThat(
            sesiones.existsByDocentePersonaIdAndSeccionIdAndFechaBetweenAndActiveTrue(
                personaId(e.docente), e.seccionB.getId(), FECHA, FECHA))
        .isFalse();
  }

  // ---------- REST ----------

  @Test
  void reporte_administrador_devuelveConsolidadoConTotales() throws Exception {
    Escenario e = escenario();
    Estudiante alumno = estudiante("Ana", "Quispe", "Rojas");
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarAusente);
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 1),
        AsistenciaEstudiante::marcarPresente);

    mvc.perform(
            get(RUTA)
                .param("desde", FECHA.toString())
                .param("hasta", FECHA.toString())
                .param("seccionId", e.seccionA.getId().toString())
                .with(user(administrador()).roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.desde").value("2026-11-04"))
        .andExpect(jsonPath("$.totales.totalSesiones").value(2))
        .andExpect(jsonPath("$.alumnos[0].nombreCompleto").value("Ana Quispe Rojas"))
        .andExpect(jsonPath("$.alumnos[0].inasistencias").value(1))
        .andExpect(jsonPath("$.alumnos[0].porcentajeInasistencia").value(50.0));
  }

  @Test
  void reporte_sinDesde_responde400() throws Exception {
    mvc.perform(
            get(RUTA)
                .param("hasta", FECHA.toString())
                .with(user(administrador()).roles("ADMINISTRADOR")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void reporte_rangoInvertido_responde422() throws Exception {
    mvc.perform(
            get(RUTA)
                .param("desde", FECHA.toString())
                .param("hasta", FECHA.minusDays(1).toString())
                .with(user(administrador()).roles("ADMINISTRADOR")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.path").value(RUTA));
  }

  @Test
  void reporte_docenteSeccionAjena_responde403() throws Exception {
    Escenario e = escenario();
    Docente ajeno = docente("ajeno");

    mvc.perform(
            get(RUTA)
                .param("desde", FECHA.toString())
                .param("hasta", FECHA.toString())
                .param("seccionId", e.seccionA.getId().toString())
                .with(user(usuarioDe(ajeno)).roles("DOCENTE")))
        .andExpect(status().isForbidden());
  }

  @Test
  void reporte_docenteConGradoAjenoSinSeccion_responde200Vacio() throws Exception {
    Escenario e = escenario();
    Estudiante alumno = estudiante("Noa", "Paz", "Rios");
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarAusente);
    Docente ajeno = docente("ajeno");

    mvc.perform(
            get(RUTA)
                .param("desde", FECHA.toString())
                .param("hasta", FECHA.toString())
                .param("gradoId", e.seccionA.getGrado().getId().toString())
                .with(user(usuarioDe(ajeno)).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.alumnos.length()").value(0))
        .andExpect(jsonPath("$.totales.totalSesiones").value(0));
  }

  @Test
  void reporte_alumno_responde403() throws Exception {
    mvc.perform(
            get(RUTA)
                .param("desde", FECHA.toString())
                .param("hasta", FECHA.toString())
                .with(user("alumno-reporte").roles("ALUMNO")))
        .andExpect(status().isForbidden());
  }

  @Test
  void csv_administrador_respondeTextCsvConAdjunto() throws Exception {
    Escenario e = escenario();
    Estudiante alumno = estudiante("Ana", "Muñoz", "Rojas");
    marcar(
        alumno,
        sesion(e.seccionA, e.area, e.docente, FECHA, 0),
        AsistenciaEstudiante::marcarTardanza);

    mvc.perform(
            get(RUTA + "/csv")
                .param("desde", "2026-11-01")
                .param("hasta", "2026-11-17")
                .param("seccionId", e.seccionA.getId().toString())
                .with(user(administrador()).roles("ADMINISTRADOR")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", containsString("text/csv")))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    containsString("reporte-asistencia_2026-11-01_2026-11-17.csv")))
        .andExpect(content().string(containsString("Ana Muñoz Rojas")));
  }

  @Test
  void secciones_docente_soloSusSecciones() throws Exception {
    Escenario e = escenario();
    Docente tutor = docente("tutor");
    sesion(e.seccionA, e.area, tutor, FECHA, 0);

    mvc.perform(get(RUTA + "/secciones").with(user(usuarioDe(tutor)).roles("DOCENTE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].seccionId").value(e.seccionA.getId()))
        .andExpect(jsonPath("$[0].gradoId").value(e.seccionA.getGrado().getId()))
        .andExpect(jsonPath("$[0].seccion").value("A"));
  }

  // ---------- Fixtures ----------

  private record Escenario(Seccion seccionA, Seccion seccionB, Area area, Docente docente) {}

  private Escenario escenario() {
    Grado grado = new Grado();
    grado.setNumero(NUMERO_GRADO.getAndIncrement());
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccionA = new Seccion();
    seccionA.setCodigo("A");
    seccionA.setVacantesTotales(30);
    Seccion seccionB = new Seccion();
    seccionB.setCodigo("B");
    seccionB.setVacantesTotales(30);
    grado.agregarSeccion(seccionA);
    grado.agregarSeccion(seccionB);
    entityManager.persist(grado);
    return new Escenario(seccionA, seccionB, area(), docente("titular"));
  }

  private Area area() {
    Area area = new Area();
    area.setCodigo("REP-" + sufijo().substring(0, 8));
    area.setNombre("Área de reporte");
    entityManager.persist(area);
    return area;
  }

  private Docente docente(String nombre) {
    Persona persona = new Persona("D" + sufijo(), nombre, "Prueba", "Docente");
    entityManager.persist(persona);
    Docente docente = new Docente();
    docente.setPersona(persona);
    entityManager.persist(docente);
    entityManager.persist(
        new CuentaUsuario("doc" + persona.getDni().toLowerCase(), "hash", Rol.DOCENTE, persona));
    return docente;
  }

  private String administrador() {
    Persona persona = new Persona("A" + sufijo(), "Admin", "Prueba", "Reporte");
    entityManager.persist(persona);
    String usuario = "adm" + persona.getDni().toLowerCase();
    entityManager.persist(new CuentaUsuario(usuario, "hash", Rol.ADMINISTRADOR, persona));
    entityManager.flush();
    return usuario;
  }

  private String usuarioDe(Docente docente) {
    entityManager.flush();
    return "doc" + docente.getPersona().getDni().toLowerCase();
  }

  private Integer personaId(Docente docente) {
    return docente.getPersona().getId();
  }

  private Estudiante estudiante(String nombre, String paterno, String materno) {
    Persona persona = new Persona(sufijo(), nombre, paterno, materno);
    entityManager.persist(persona);
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    entityManager.persist(estudiante);
    return estudiante;
  }

  private SesionClase sesion(
      Seccion seccion, Area area, Docente docente, LocalDate fecha, int orden) {
    SesionClase sesion = new SesionClase();
    sesion.setSeccion(seccion);
    sesion.setArea(area);
    sesion.setDocente(docente);
    sesion.setFecha(fecha);
    sesion.setHoraInicio(HORA.plusHours(orden));
    sesion.setHoraFin(HORA.plusHours(orden).plusMinutes(45));
    entityManager.persist(sesion);
    return sesion;
  }

  private AsistenciaEstudiante marcar(
      Estudiante estudiante, SesionClase sesion, Consumer<AsistenciaEstudiante> estado) {
    AsistenciaEstudiante asistencia = new AsistenciaEstudiante();
    asistencia.setEstudiante(estudiante);
    asistencia.setSesionClase(sesion);
    estado.accept(asistencia);
    entityManager.persist(asistencia);
    entityManager.flush();
    return asistencia;
  }

  private static String sufijo() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
}
