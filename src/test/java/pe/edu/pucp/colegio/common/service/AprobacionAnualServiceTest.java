package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.SituacionMatriculaResponse;
import pe.edu.pucp.colegio.common.dto.SituacionMatriculaResponse.TipoSituacion;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.model.Competencia;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.CalificacionRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

class AprobacionAnualServiceTest {

  private static final Integer ESTUDIANTE_ID = 10;
  private static final Integer PERIODO_ANTERIOR_ID = 25;
  private static final Integer MATRICULA_ANTERIOR_ID = 80;
  private final MatriculaRepository matriculas = mock(MatriculaRepository.class);
  private final CalificacionRepository calificaciones = mock(CalificacionRepository.class);
  private final EstudianteRepository estudiantes = mock(EstudianteRepository.class);
  private final AprobacionAnualService aprobacion =
      new AprobacionAnualService(matriculas, calificaciones, estudiantes);
  private final Matricula anterior = new Matricula();
  private Periodo destino;

  @BeforeEach
  void preparar() {
    Grado gradoAnterior = new Grado();
    gradoAnterior.setNumero(2);
    Periodo periodoAnterior = new Periodo();
    periodoAnterior.setFechaInicio(LocalDate.of(2025, 3, 1));
    periodoAnterior.setFechaFin(LocalDate.of(2025, 12, 20));
    periodoAnterior.setGrado(gradoAnterior);
    periodoAnterior.setActasCerradas(true);
    ReflectionTestUtils.setField(periodoAnterior, "id", PERIODO_ANTERIOR_ID);
    anterior.setPeriodo(periodoAnterior);
    anterior.setEstado(EstadoMatricula.CONFIRMADA);
    anterior.setActive(true);
    ReflectionTestUtils.setField(anterior, "id", MATRICULA_ANTERIOR_ID);

    Grado gradoDestino = new Grado();
    gradoDestino.setNumero(3);
    destino = new Periodo();
    destino.setFechaInicio(LocalDate.of(2026, 3, 1));
    destino.setFechaFin(LocalDate.of(2026, 12, 20));
    destino.setGrado(gradoDestino);

    when(matriculas.findAllByEstudianteIdIn(List.of(ESTUDIANTE_ID))).thenReturn(List.of(anterior));
  }

  @Test
  void permiteAvanceCuandoMenosDeCuatroAreasTienenCEnLaMitadDeSusCompetencias() {
    when(calificaciones
            .findAllByPeriodoIdAndMatriculaIdAndConsolidadaTrueAndBimestreIsNullAndActiveTrue(
                PERIODO_ANTERIOR_ID, MATRICULA_ANTERIOR_ID))
        .thenReturn(notas(3));

    assertDoesNotThrow(() -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
  }

  @Test
  void bloqueaAvanceCuandoCuatroAreasTienenCEnLaMitadDeSusCompetencias() {
    when(calificaciones
            .findAllByPeriodoIdAndMatriculaIdAndConsolidadaTrueAndBimestreIsNullAndActiveTrue(
                PERIODO_ANTERIOR_ID, MATRICULA_ANTERIOR_ID))
        .thenReturn(notas(4));

    assertThrows(
        ResponseStatusException.class, () -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
  }

  @Test
  void bloqueaAvanceSiLasActasDelAnioAnteriorSiguenAbiertas() {
    anterior.getPeriodo().setActasCerradas(false);
    when(calificaciones
            .findAllByPeriodoIdAndMatriculaIdAndConsolidadaTrueAndBimestreIsNullAndActiveTrue(
                PERIODO_ANTERIOR_ID, MATRICULA_ANTERIOR_ID))
        .thenReturn(notas(0));

    assertThrows(
        ResponseStatusException.class, () -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
  }

  @Test
  void quienDesapruebaPuedeRepetirElMismoGrado() {
    conNotas(4);
    destino.getGrado().setNumero(2);

    assertDoesNotThrow(() -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
  }

  @Test
  void quienApruebaNoPuedeRepetirNiBajarDeGrado() {
    conNotas(3);
    destino.getGrado().setNumero(2);

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
    assertTrue(error.getReason().contains("debe matricularse en 3.º"));
  }

  @Test
  void elMensajeIncluyeElNombreDelEstudiante() {
    Persona persona = new Persona("70000000", "Lucía", "Ramos", "Díaz");
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    anterior.setEstudiante(estudiante);
    conNotas(4);

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
    assertEquals(
        "Lucía Ramos Díaz no aprobó 2.º en 2025: debe matricularse otra vez en 2.º",
        error.getReason());
  }

  @Test
  void quienApruebaQuintoEgresa() {
    anterior.getPeriodo().getGrado().setNumero(5);
    conNotas(0);
    destino.getGrado().setNumero(5);

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
    assertTrue(error.getReason().contains("ya egresó"));
  }

  @Test
  void unEstudianteNuevoEntraACualquierGrado() {
    when(matriculas.findAllByEstudianteIdIn(List.of(ESTUDIANTE_ID))).thenReturn(List.of());
    destino.getGrado().setNumero(4);

    assertDoesNotThrow(() -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
  }

  @Test
  void sinNotasAnualesNoSePuedeDecidir() {
    conNotas(-1);

    ResponseStatusException error =
        assertThrows(
            ResponseStatusException.class, () -> aprobacion.validarDestino(ESTUDIANTE_ID, destino));
    assertTrue(error.getReason().contains("no tiene notas anuales consolidadas de 2025"));
  }

  @Test
  void listaLaSituacionDeCadaEstudiante() {
    Estudiante conHistorial = new Estudiante();
    ReflectionTestUtils.setField(conHistorial, "id", ESTUDIANTE_ID);
    conHistorial.getMatriculas().add(anterior);
    Estudiante nuevo = new Estudiante();
    ReflectionTestUtils.setField(nuevo, "id", 11);
    when(estudiantes.findAllByActiveTrueOrderByIdAsc()).thenReturn(List.of(conHistorial, nuevo));
    conNotas(0);

    List<SituacionMatriculaResponse> situaciones = aprobacion.situaciones(2026);

    assertEquals(TipoSituacion.PROMOVIDO, situaciones.get(0).situacion());
    assertEquals(2, situaciones.get(0).gradoAnterior());
    assertEquals(3, situaciones.get(0).gradoDestino());
    assertEquals(TipoSituacion.NUEVO, situaciones.get(1).situacion());
  }

  @Test
  void conActasAbiertasLaSituacionQuedaBloqueada() {
    anterior.getPeriodo().setActasCerradas(false);
    Estudiante conHistorial = new Estudiante();
    ReflectionTestUtils.setField(conHistorial, "id", ESTUDIANTE_ID);
    conHistorial.getMatriculas().add(anterior);
    when(estudiantes.findAllByActiveTrueOrderByIdAsc()).thenReturn(List.of(conHistorial));

    SituacionMatriculaResponse situacion = aprobacion.situaciones(2026).get(0);

    assertEquals(TipoSituacion.BLOQUEADO, situacion.situacion());
    assertEquals("faltan cerrar las actas de 2025", situacion.motivo());
  }

  private void conNotas(int areasConDesaprobacion) {
    when(calificaciones
            .findAllByPeriodoIdAndMatriculaIdAndConsolidadaTrueAndBimestreIsNullAndActiveTrue(
                PERIODO_ANTERIOR_ID, MATRICULA_ANTERIOR_ID))
        .thenReturn(areasConDesaprobacion < 0 ? List.of() : notas(areasConDesaprobacion));
  }

  private List<Calificacion> notas(int areasConDesaprobacion) {
    List<Calificacion> resultado = new ArrayList<>();
    for (int areaNumero = 1; areaNumero <= 4; areaNumero++) {
      Area area = new Area();
      area.setCodigo("AREA_" + areaNumero);
      for (int competenciaNumero = 1; competenciaNumero <= 4; competenciaNumero++) {
        Competencia competencia = new Competencia();
        competencia.setCodigo("COMP_" + areaNumero + "_" + competenciaNumero);
        competencia.setAreas(new HashSet<>(List.of(area)));
        area.setCompetencias(new HashSet<>(List.of(competencia)));
        Calificacion nota = new Calificacion();
        nota.setCompetencia(competencia);
        nota.setNota(areaNumero <= areasConDesaprobacion && competenciaNumero <= 2 ? "C" : "B");
        nota.setConsolidada(true);
        nota.setBimestre(null);
        nota.setActive(true);
        resultado.add(nota);
      }
    }
    return resultado;
  }
}
