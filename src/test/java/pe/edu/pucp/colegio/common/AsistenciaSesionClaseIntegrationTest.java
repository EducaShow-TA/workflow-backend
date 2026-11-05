package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.AsistenciaDocente;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionClase;

@SpringBootTest
@Transactional
class AsistenciaSesionClaseIntegrationTest {

  private static final AtomicInteger NUMERO_GRADO = new AtomicInteger(20_000);

  @PersistenceContext EntityManager entityManager;

  @Test
  void persisteLaAsistenciaDelAlumnoRelacionadaConLaSesionYElEstudiante() {
    DatosSesion datos = crearDatosSesion();
    AsistenciaEstudiante asistencia = new AsistenciaEstudiante();
    asistencia.setEstudiante(datos.estudiante());
    asistencia.setSesionClase(datos.sesion());
    asistencia.marcarTardanza();

    entityManager.persist(asistencia);
    entityManager.flush();
    Integer asistenciaId = asistencia.getId();
    entityManager.clear();

    AsistenciaEstudiante guardada = entityManager.find(AsistenciaEstudiante.class, asistenciaId);
    assertEquals(EstadoAsistencia.TARDANZA, guardada.getEstadoAsistencia());
    assertEquals(datos.estudiante().getId(), guardada.getEstudiante().getId());
    assertEquals(datos.sesion().getId(), guardada.getSesionClase().getId());
    assertEquals(1, guardada.getEstudiante().getAsistencias().size());
    assertEquals(1, guardada.getSesionClase().getAsistenciasEstudiantes().size());
  }

  @Test
  void noPermiteRegistrarDosVecesAlMismoAlumnoEnUnaSesion() {
    DatosSesion datos = crearDatosSesion();
    AsistenciaEstudiante primera = crearAsistencia(datos);
    entityManager.persist(primera);
    entityManager.flush();

    AsistenciaEstudiante duplicada = crearAsistencia(datos);
    assertThrows(
        PersistenceException.class,
        () -> {
          entityManager.persist(duplicada);
          entityManager.flush();
        });
  }

  @Test
  void persisteLaAsistenciaDiariaDelDocenteYLasMarcasHorarias() {
    DatosSesion datos = crearDatosSesion();
    AsistenciaDocente asistencia = new AsistenciaDocente();
    asistencia.setDocente(datos.docente());
    asistencia.setFecha(LocalDate.of(2026, 11, 4));
    asistencia.marcarIngreso(LocalTime.of(7, 55));
    asistencia.marcarSalida(LocalTime.of(15, 0));

    entityManager.persist(asistencia);
    entityManager.flush();
    Integer asistenciaId = asistencia.getId();
    entityManager.clear();

    AsistenciaDocente guardada = entityManager.find(AsistenciaDocente.class, asistenciaId);
    assertEquals(EstadoAsistenciaDocente.PRESENTE, guardada.getEstado());
    assertEquals(datos.docente().getId(), guardada.getDocente().getId());
    assertEquals(LocalTime.of(7, 55), guardada.getHoraEntrada());
    assertEquals(LocalTime.of(15, 0), guardada.getHoraSalida());
    assertEquals(datos.docente().getPersona().getId(), guardada.getDocente().getPersona().getId());
  }

  @Test
  void justificarAsistenciaRequiereMotivoYGuardaElArchivoOpcional() {
    AsistenciaEstudiante asistencia = new AsistenciaEstudiante();

    assertThrows(IllegalArgumentException.class, () -> asistencia.justificar("  ", null));

    asistencia.justificar(" Cita médica ", " constancia.pdf ");
    assertEquals(EstadoAsistencia.JUSTIFICADA, asistencia.getEstadoAsistencia());
    assertEquals("Cita médica", asistencia.getMotivoJustificacion());
    assertEquals("constancia.pdf", asistencia.getArchivoJustificacion());
  }

  private DatosSesion crearDatosSesion() {
    String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    Persona personaEstudiante = new Persona(sufijo, "Ana", "Prueba", "Alumno");
    Persona personaDocente = new Persona("D" + sufijo, "Luis", "Prueba", "Docente");
    entityManager.persist(personaEstudiante);
    entityManager.persist(personaDocente);

    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(personaEstudiante);
    entityManager.persist(estudiante);

    Docente docente = new Docente();
    docente.setPersona(personaDocente);
    entityManager.persist(docente);

    Area area = new Area();
    area.setCodigo("ASIS-" + sufijo.substring(0, 8));
    area.setNombre("Área de prueba");
    entityManager.persist(area);

    Grado grado = new Grado();
    grado.setNumero(NUMERO_GRADO.getAndIncrement());
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);
    Seccion seccion = new Seccion();
    seccion.setCodigo("A");
    seccion.setVacantesTotales(30);
    grado.agregarSeccion(seccion);
    entityManager.persist(grado);

    SesionClase sesion = new SesionClase();
    sesion.setSeccion(seccion);
    sesion.setArea(area);
    sesion.setDocente(docente);
    sesion.setFecha(LocalDate.of(2026, 11, 4));
    sesion.setHoraInicio(LocalTime.of(8, 0));
    sesion.setHoraFin(LocalTime.of(8, 45));
    entityManager.persist(sesion);
    entityManager.flush();
    assertNotNull(sesion.getId());
    return new DatosSesion(estudiante, docente, sesion);
  }

  private AsistenciaEstudiante crearAsistencia(DatosSesion datos) {
    AsistenciaEstudiante asistencia = new AsistenciaEstudiante();
    asistencia.setEstudiante(datos.estudiante());
    asistencia.setSesionClase(datos.sesion());
    asistencia.marcarPresente();
    return asistencia;
  }

  private record DatosSesion(Estudiante estudiante, Docente docente, SesionClase sesion) {}
}
