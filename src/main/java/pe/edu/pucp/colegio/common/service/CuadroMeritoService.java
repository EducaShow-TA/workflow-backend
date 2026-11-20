package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.CuadroMeritoResponse;
import pe.edu.pucp.colegio.common.dto.CuadroMeritoResponse.EstudianteRanking;
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.CalificacionRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

@Service
public class CuadroMeritoService {

  private final PeriodoRepository periodos;
  private final MatriculaRepository matriculas;
  private final CalificacionRepository calificaciones;

  public CuadroMeritoService(
      PeriodoRepository periodos,
      MatriculaRepository matriculas,
      CalificacionRepository calificaciones) {
    this.periodos = periodos;
    this.matriculas = matriculas;
    this.calificaciones = calificaciones;
  }

  @Transactional(readOnly = true)
  public CuadroMeritoResponse generar(Integer periodoId) {
    Periodo periodo = buscarPeriodo(periodoId);
    return generar(periodoId, List.of(periodo), periodo.getSeccion().getCodigo());
  }

  /** Genera el ranking de todo un grado o de una de sus secciones para el año indicado. */
  @Transactional(readOnly = true)
  public CuadroMeritoResponse generar(Integer gradoId, Integer anio, Integer seccionId) {
    if (gradoId == null || anio == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Indique gradoId y anio para generar el cuadro por grado");
    }
    List<Periodo> periodosDelGrado =
        periodos.findAllByGradoIdAndActiveTrue(gradoId).stream()
            .filter(p -> p.getFechaInicio().getYear() == anio)
            .filter(p -> seccionId == null || p.getSeccion().getId().equals(seccionId))
            .sorted(Comparator.comparing(Periodo::getFechaInicio))
            .toList();
    if (periodosDelGrado.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "No hay períodos para ese grado y año");
    }
    for (Periodo periodo : periodosDelGrado) {
      exigirActasCerradas(periodo);
    }
    String seccion = seccionId == null ? "Todas" : periodosDelGrado.get(0).getSeccion().getCodigo();
    return generar(null, periodosDelGrado, seccion);
  }

  private CuadroMeritoResponse generar(
      Integer periodoId, List<Periodo> periodosIncluidos, String seccion) {
    Map<Integer, Matricula> roster = new LinkedHashMap<>();
    Map<Integer, Map<String, List<BigDecimal>>> puntajes = new HashMap<>();
    for (Periodo periodo : periodosIncluidos) {
      List<Matricula> matriculasPeriodo =
          matriculas.findAllByPeriodoIdAndActiveTrueAndEstadoIn(
              periodo.getId(), List.of(EstadoMatricula.CONFIRMADA));
      matriculasPeriodo.forEach(m -> roster.putIfAbsent(m.getEstudiante().getId(), m));
      agregarPuntajesPeriodo(periodo, puntajes);
    }

    List<Entrada> entradas =
        roster.values().stream()
            .map(
                matricula ->
                    new Entrada(
                        matricula,
                        new Acumulado(
                            puntajes.getOrDefault(matricula.getEstudiante().getId(), Map.of()))))
            .sorted(this::compararEntradas)
            .toList();

    List<EstudianteRanking> estudiantes = new ArrayList<>(entradas.size());
    Entrada anteriorConPuesto = null;
    Integer puestoActual = null;
    for (int indice = 0; indice < entradas.size(); indice++) {
      Entrada entrada = entradas.get(indice);
      if (entrada.acumulado().cantidad() == 0) {
        estudiantes.add(respuesta(entrada, null));
        continue;
      }
      if (anteriorConPuesto == null || !mismoPromedio(anteriorConPuesto, entrada)) {
        puestoActual = indice + 1;
      }
      estudiantes.add(respuesta(entrada, puestoActual));
      anteriorConPuesto = entrada;
    }

    Periodo primero = periodosIncluidos.get(0);
    Periodo ultimo = periodosIncluidos.get(periodosIncluidos.size() - 1);
    return new CuadroMeritoResponse(
        periodoId,
        primero.getFechaInicio().getYear(),
        primero.getGrado().getNumero(),
        primero.getGrado().getNivelEducativo().name(),
        seccion,
        primero.getFechaInicio(),
        ultimo.getFechaFin(),
        estudiantes);
  }

  private Periodo buscarPeriodo(Integer periodoId) {
    Periodo periodo =
        periodos
            .findById(periodoId)
            .filter(Periodo::getActive)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Período no encontrado"));
    exigirActasCerradas(periodo);
    return periodo;
  }

  private void exigirActasCerradas(Periodo periodo) {
    if (!Boolean.TRUE.equals(periodo.getActasCerradas())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El cuadro de mérito solo está disponible con las actas cerradas");
    }
  }

  private void agregarPuntajesPeriodo(
      Periodo periodo, Map<Integer, Map<String, List<BigDecimal>>> puntajes) {
    Map<Integer, Map<String, List<BigDecimal>>> filasPeriodo = new HashMap<>();
    Map<Integer, Map<String, BigDecimal>> consolidadas = new HashMap<>();
    for (Calificacion calificacion :
        calificaciones.findAllByPeriodoIdAndActiveTrue(periodo.getId())) {
      Integer estudianteId = calificacion.getMatricula().getEstudiante().getId();
      String competencia = calificacion.getCompetencia().getCodigo();
      BigDecimal puntaje = puntajeFinal(calificacion);
      if (puntaje == null) {
        continue;
      }
      if (Boolean.TRUE.equals(calificacion.getConsolidada())) {
        consolidadas
            .computeIfAbsent(estudianteId, ignored -> new HashMap<>())
            .put(competencia, puntaje);
      } else {
        filasPeriodo
            .computeIfAbsent(estudianteId, ignored -> new HashMap<>())
            .computeIfAbsent(competencia, ignored -> new ArrayList<>())
            .add(puntaje);
      }
    }

    for (var estudiante : filasPeriodo.entrySet()) {
      Map<String, BigDecimal> notasConsolidadas =
          consolidadas.getOrDefault(estudiante.getKey(), Map.of());
      for (var competencia : estudiante.getValue().entrySet()) {
        BigDecimal puntaje = notasConsolidadas.get(competencia.getKey());
        if (puntaje == null) {
          puntaje = promedio(competencia.getValue());
        }
        puntajes
            .computeIfAbsent(estudiante.getKey(), ignored -> new HashMap<>())
            .computeIfAbsent(competencia.getKey(), ignored -> new ArrayList<>())
            .add(puntaje);
      }
    }
    for (var estudiante : consolidadas.entrySet()) {
      for (var competencia : estudiante.getValue().entrySet()) {
        if (filasPeriodo
            .getOrDefault(estudiante.getKey(), Map.of())
            .containsKey(competencia.getKey())) {
          continue;
        }
        puntajes
            .computeIfAbsent(estudiante.getKey(), ignored -> new HashMap<>())
            .computeIfAbsent(competencia.getKey(), ignored -> new ArrayList<>())
            .add(competencia.getValue());
      }
    }
  }

  private BigDecimal puntajeFinal(Calificacion calificacion) {
    if (Boolean.TRUE.equals(calificacion.getConsolidada())) {
      return calificacion.getPuntajeConsolidado();
    }
    return switch (calificacion.getNota()) {
      case "C" -> BigDecimal.ONE;
      case "B" -> BigDecimal.valueOf(2);
      case "A" -> BigDecimal.valueOf(3);
      case "AD" -> BigDecimal.valueOf(4);
      default -> null;
    };
  }

  private BigDecimal promedio(List<BigDecimal> valores) {
    return valores.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(valores.size()), 8, RoundingMode.HALF_UP);
  }

  private int compararEntradas(Entrada primero, Entrada segundo) {
    boolean primeroTienePromedio = primero.acumulado().cantidad() > 0;
    boolean segundoTienePromedio = segundo.acumulado().cantidad() > 0;
    if (primeroTienePromedio != segundoTienePromedio) {
      return primeroTienePromedio ? -1 : 1;
    }
    if (primeroTienePromedio) {
      int porPromedio = compararPromedios(segundo, primero);
      if (porPromedio != 0) {
        return porPromedio;
      }
    }
    return primero
        .matricula()
        .getEstudiante()
        .getPersona()
        .getNombreCompleto()
        .compareToIgnoreCase(segundo.matricula().getEstudiante().getPersona().getNombreCompleto());
  }

  private int compararPromedios(Entrada primero, Entrada segundo) {
    return primero
        .acumulado()
        .suma()
        .multiply(BigDecimal.valueOf(segundo.acumulado().cantidad()))
        .compareTo(
            segundo
                .acumulado()
                .suma()
                .multiply(BigDecimal.valueOf(primero.acumulado().cantidad())));
  }

  private boolean mismoPromedio(Entrada primero, Entrada segundo) {
    return compararPromedios(primero, segundo) == 0;
  }

  private EstudianteRanking respuesta(Entrada entrada, Integer puesto) {
    Acumulado acumulado = entrada.acumulado();
    BigDecimal promedio =
        acumulado.cantidad() == 0
            ? null
            : acumulado
                .suma()
                .divide(BigDecimal.valueOf(acumulado.cantidad()), 4, RoundingMode.HALF_UP);
    Matricula matricula = entrada.matricula();
    return new EstudianteRanking(
        puesto,
        matricula.getId(),
        matricula.getEstudiante().getId(),
        matricula.getEstudiante().getPersona().getNombreCompleto(),
        matricula.getPeriodo().getSeccion().getCodigo(),
        promedio,
        acumulado.cantidad());
  }

  private record Entrada(Matricula matricula, Acumulado acumulado) {}

  private static final class Acumulado {
    private BigDecimal suma = BigDecimal.ZERO;
    private int cantidad;

    private Acumulado(Map<String, List<BigDecimal>> porCompetencia) {
      porCompetencia.forEach(
          (competencia, valores) -> {
            BigDecimal puntaje =
                valores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(valores.size()), 8, RoundingMode.HALF_UP);
            suma = suma.add(puntaje);
            cantidad++;
          });
    }

    private BigDecimal suma() {
      return suma;
    }

    private int cantidad() {
      return cantidad;
    }
  }
}
