package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse.BoletaHistorial;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse.CompromisoHistorial;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse.MatriculaHistorial;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse.Totales;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.ComprobantePago;
import pe.edu.pucp.colegio.common.model.CompromisoPago;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.repository.BoletaPensionRepository;
import pe.edu.pucp.colegio.common.repository.ComprobantePagoRepository;
import pe.edu.pucp.colegio.common.repository.CompromisoPagoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

/** Completa las boletas de un estudiante hasta el mes actual y devuelve todo su historial. */
@Service
public class HistorialBoletasService {

  private static final Logger log = LoggerFactory.getLogger(HistorialBoletasService.class);

  private final EstudianteRepository estudiantes;
  private final MatriculaRepository matriculas;
  private final CompromisoPagoRepository compromisos;
  private final BoletaPensionRepository boletas;
  private final ComprobantePagoRepository comprobantes;
  private final FinanzasService finanzas;

  public HistorialBoletasService(
      EstudianteRepository estudiantes,
      MatriculaRepository matriculas,
      CompromisoPagoRepository compromisos,
      BoletaPensionRepository boletas,
      ComprobantePagoRepository comprobantes,
      FinanzasService finanzas) {
    this.estudiantes = estudiantes;
    this.matriculas = matriculas;
    this.compromisos = compromisos;
    this.boletas = boletas;
    this.comprobantes = comprobantes;
    this.finanzas = finanzas;
  }

  @Transactional
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public HistorialBoletasResponse generar(Integer estudianteId) {
    Estudiante estudiante =
        estudiantes
            .findById(estudianteId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el estudiante indicado"));
    var completadas = finanzas.completarBoletasDeEstudiante(estudianteId);
    List<Matricula> visibles = listarMatriculas(estudianteId);
    Map<Integer, List<BoletaPension>> boletasPorMatricula =
        boletas
            .findAllByCompromisoMatriculaEstudianteIdAndActiveTrueOrderByMesCobroAsc(estudianteId)
            .stream()
            .collect(Collectors.groupingBy(b -> b.getCompromiso().getMatricula().getId()));
    Map<Integer, CompromisoPago> compromisoPorMatricula =
        compromisos.findAllByMatriculaEstudianteId(estudianteId).stream()
            .collect(Collectors.toMap(c -> c.getMatricula().getId(), Function.identity()));
    Map<Integer, String> numeroPorBoleta = numerosDeComprobante(boletasPorMatricula);

    List<MatriculaHistorial> filas =
        visibles.stream()
            .map(
                m ->
                    fila(
                        m,
                        compromisoPorMatricula.get(m.getId()),
                        boletasPorMatricula.getOrDefault(m.getId(), List.of()),
                        numeroPorBoleta))
            .toList();
    log.info(
        "event=historial_boletas_generado estudianteId={} generadas={} enMora={}",
        estudianteId,
        completadas.generadas(),
        completadas.enMora());
    return new HistorialBoletasResponse(
        estudianteId,
        estudiante.getPersona().getNombreCompleto(),
        completadas.mesCorte(),
        completadas.generadas(),
        completadas.enMora(),
        totalizar(filas),
        filas);
  }

  private List<Matricula> listarMatriculas(Integer estudianteId) {
    return matriculas.findAllByEstudianteIdIn(List.of(estudianteId)).stream()
        .filter(
            m -> Boolean.TRUE.equals(m.getActive()) || m.getEstado() == EstadoMatricula.RETIRADA)
        .sorted(
            Comparator.comparing((Matricula m) -> m.getPeriodo().getFechaInicio())
                .thenComparing(Matricula::getId)
                .reversed())
        .toList();
  }

  private Map<Integer, String> numerosDeComprobante(
      Map<Integer, List<BoletaPension>> boletasPorMatricula) {
    List<Integer> ids =
        boletasPorMatricula.values().stream()
            .flatMap(List::stream)
            .map(BoletaPension::getId)
            .toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return comprobantes.findAllByBoletaIdInAndActiveTrue(ids).stream()
        .collect(
            Collectors.toMap(
                c -> c.getBoleta().getId(),
                ComprobantePago::getNumeroComprobante,
                (primero, repetido) -> primero));
  }

  private MatriculaHistorial fila(
      Matricula matricula,
      CompromisoPago compromiso,
      List<BoletaPension> emitidas,
      Map<Integer, String> numeroPorBoleta) {
    var periodo = matricula.getPeriodo();
    return new MatriculaHistorial(
        matricula.getId(),
        periodo.getFechaInicio().getYear(),
        periodo.getGrado().getNumero(),
        periodo.getSeccion().getCodigo(),
        matricula.getFechaMatricula(),
        matricula.getEstado(),
        compromiso == null
            ? null
            : new CompromisoHistorial(
                compromiso.getId(),
                compromiso.getConcepto(),
                compromiso.getNumeroCuotas(),
                compromiso.getMesInicio()),
        emitidas.stream()
            .map(
                b ->
                    new BoletaHistorial(
                        b.getId(),
                        b.getMesCobro(),
                        b.getFechaVencimiento(),
                        b.calcularMontoTotal(),
                        b.getEstadoPago(),
                        b.getMecanismoGeneracion(),
                        numeroPorBoleta.get(b.getId())))
            .toList());
  }

  /** La deuda de una matrícula retirada no se cobra: igual que en el reporte de cobranza. */
  private Totales totalizar(List<MatriculaHistorial> filas) {
    List<MatriculaHistorial> cobrables =
        filas.stream().filter(m -> m.estado() != EstadoMatricula.RETIRADA).toList();
    return new Totales(
        sumar(filas, EstadoBoleta.PAGADA),
        sumar(cobrables, EstadoBoleta.PENDIENTE),
        sumar(cobrables, EstadoBoleta.EN_MORA));
  }

  private BigDecimal sumar(List<MatriculaHistorial> filas, EstadoBoleta estado) {
    return filas.stream()
        .flatMap(m -> m.boletas().stream())
        .filter(b -> b.estadoPago() == estado)
        .map(BoletaHistorial::montoTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2);
  }
}
