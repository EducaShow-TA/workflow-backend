package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
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
    Periodo periodo =
        periodos
            .findById(periodoId)
            .filter(Periodo::getActive)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Período no encontrado"));
    if (!Boolean.TRUE.equals(periodo.getActasCerradas())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El cuadro de mérito solo está disponible con las actas cerradas");
    }

    List<Matricula> roster =
        matriculas.findAllByPeriodoIdAndActiveTrueAndEstadoIn(
            periodoId, List.of(EstadoMatricula.CONFIRMADA));
    Map<Integer, Acumulado> acumulados = new HashMap<>();
    for (Calificacion calificacion : calificaciones.findAllByPeriodoIdAndActiveTrue(periodoId)) {
      BigDecimal puntaje = puntajeFinal(calificacion);
      if (puntaje != null) {
        acumulados
            .computeIfAbsent(calificacion.getMatricula().getId(), ignored -> new Acumulado())
            .agregar(puntaje);
      }
    }

    List<Entrada> entradas =
        roster.stream()
            .map(
                matricula ->
                    new Entrada(
                        matricula, acumulados.getOrDefault(matricula.getId(), new Acumulado())))
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

    return new CuadroMeritoResponse(
        periodoId,
        periodo.getFechaInicio().getYear(),
        periodo.getGrado().getNumero(),
        periodo.getGrado().getNivelEducativo().name(),
        periodo.getSeccion().getCodigo(),
        periodo.getFechaInicio(),
        periodo.getFechaFin(),
        estudiantes);
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
        promedio,
        acumulado.cantidad());
  }

  private record Entrada(Matricula matricula, Acumulado acumulado) {}

  private static final class Acumulado {
    private BigDecimal suma = BigDecimal.ZERO;
    private int cantidad;

    private void agregar(BigDecimal puntaje) {
      suma = suma.add(puntaje);
      cantidad++;
    }

    private BigDecimal suma() {
      return suma;
    }

    private int cantidad() {
      return cantidad;
    }
  }
}
