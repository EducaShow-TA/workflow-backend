package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.BimestreFechasRequest;
import pe.edu.pucp.colegio.common.dto.BimestreResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.BimestreCalendario;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.BimestreCalendarioRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

/** Calendario de bimestres de cada año escolar; sin calendario se usan cuatro tramos iguales. */
@Service
public class CalendarioBimestralService {

  public static final int BIMESTRES = 4;
  private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final BimestreCalendarioRepository bimestres;
  private final PeriodoRepository periodos;
  private final Clock clock;

  public CalendarioBimestralService(
      BimestreCalendarioRepository bimestres, PeriodoRepository periodos, Clock clock) {
    this.bimestres = bimestres;
    this.periodos = periodos;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public int actual(Periodo periodo, LocalDate fecha) {
    return calculadora(periodo).applyAsInt(fecha);
  }

  /** Para recorrer muchas fechas de un mismo período con una sola consulta del calendario. */
  @Transactional(readOnly = true)
  public ToIntFunction<LocalDate> calculadora(Periodo periodo) {
    List<BimestreCalendario> calendario =
        bimestres.findAllByAnhioOrderByNumero(periodo.getFechaInicio().getYear());
    if (calendario.size() != BIMESTRES) {
      return fecha -> BimestreAcademico.actual(periodo, fecha);
    }
    return fecha -> bimestreEn(calendario, fecha);
  }

  @Transactional(readOnly = true)
  public boolean cerrado(Periodo periodo, Integer bimestre) {
    return bimestre != null
        && bimestres.existsByAnhioAndNumeroAndCerradoTrue(
            periodo.getFechaInicio().getYear(), bimestre);
  }

  /** El docente no registra ni rectifica en un bimestre cerrado; el administrador sí rectifica. */
  @Transactional(readOnly = true)
  public void exigirRegistroPermitido(Periodo periodo, Integer bimestre, boolean administrador) {
    if (!administrador && cerrado(periodo, bimestre)) {
      throw new BusinessException(
          "El bimestre "
              + bimestre
              + " está cerrado: solo el administrador puede rectificar sus notas");
    }
  }

  @Transactional(readOnly = true)
  public void exigirBimestreSinCerrar(Periodo periodo, Integer bimestre) {
    if (cerrado(periodo, bimestre)) {
      throw new BusinessException(
          "El bimestre "
              + bimestre
              + " está cerrado; no se pueden crear ni cambiar sus evaluaciones");
    }
  }

  @Transactional(readOnly = true)
  public List<BimestreResponse> listar(int anhio) {
    List<BimestreCalendario> calendario = bimestres.findAllByAnhioOrderByNumero(anhio);
    LocalDate hoy = LocalDate.now(clock);
    if (calendario.size() == BIMESTRES) {
      return calendario.stream().map(bimestre -> respuesta(bimestre, hoy, true)).toList();
    }
    List<Periodo> delAnhio = periodosDelAnhio(anhio);
    if (delAnhio.isEmpty()) {
      return List.of();
    }
    return proponer(anhio, inicioDe(delAnhio), finDe(delAnhio)).stream()
        .map(bimestre -> respuesta(bimestre, hoy, false))
        .toList();
  }

  /** Crea o ajusta el calendario del año al abrirlo o editar sus fechas. */
  @Transactional
  public void guardar(
      int anhio, LocalDate inicioClases, LocalDate finClases, List<BimestreFechasRequest> fechas) {
    List<BimestreCalendario> existentes = bimestres.findAllByAnhioOrderByNumero(anhio);
    List<LocalDate[]> nuevas =
        fechasPedidas(anhio, inicioClases, finClases, fechas, existentes, LocalDate.now(clock));
    validarSecuencia(inicioClases, finClases, nuevas);
    if (existentes.size() == BIMESTRES) {
      validarCambios(existentes, nuevas);
      for (int i = 0; i < BIMESTRES; i++) {
        existentes.get(i).cambiarFechas(nuevas.get(i)[0], nuevas.get(i)[1]);
      }
      bimestres.saveAll(existentes);
      return;
    }
    List<BimestreCalendario> creados = new ArrayList<>();
    for (int i = 0; i < BIMESTRES; i++) {
      creados.add(new BimestreCalendario(anhio, i + 1, nuevas.get(i)[0], nuevas.get(i)[1]));
    }
    bimestres.saveAll(creados);
  }

  /** Guarda el calendario de cuatro tramos de un año abierto antes de existir el calendario. */
  @Transactional
  public List<BimestreCalendario> asegurar(int anhio) {
    List<BimestreCalendario> calendario = bimestres.findAllByAnhioOrderByNumero(anhio);
    if (calendario.size() == BIMESTRES) {
      return calendario;
    }
    List<Periodo> delAnhio = periodosDelAnhio(anhio);
    if (delAnhio.isEmpty()) {
      throw new BusinessException("El año escolar " + anhio + " no está abierto");
    }
    return bimestres.saveAll(proponer(anhio, inicioDe(delAnhio), finDe(delAnhio)));
  }

  public List<Periodo> periodosDelAnhio(int anhio) {
    return periodos.buscar(LocalDate.of(anhio, 1, 1), LocalDate.of(anhio, 12, 31), null, null);
  }

  public BimestreResponse respuesta(BimestreCalendario bimestre) {
    return respuesta(bimestre, LocalDate.now(clock), true);
  }

  static List<BimestreCalendario> proponer(int anhio, LocalDate inicio, LocalDate fin) {
    long dias = ChronoUnit.DAYS.between(inicio, fin) + 1;
    List<BimestreCalendario> propuesta = new ArrayList<>();
    for (int numero = 1; numero <= BIMESTRES; numero++) {
      LocalDate desde = inicio.plusDays(Math.ceilDiv((numero - 1) * dias, BIMESTRES));
      LocalDate hasta =
          numero == BIMESTRES
              ? fin
              : inicio.plusDays(Math.ceilDiv(numero * dias, BIMESTRES)).minusDays(1);
      propuesta.add(new BimestreCalendario(anhio, numero, desde, hasta));
    }
    return propuesta;
  }

  private static int bimestreEn(List<BimestreCalendario> calendario, LocalDate fecha) {
    int numero = 1;
    for (BimestreCalendario bimestre : calendario) {
      if (!fecha.isBefore(bimestre.getFechaInicio())) {
        numero = bimestre.getNumero();
      }
    }
    return numero;
  }

  private BimestreResponse respuesta(
      BimestreCalendario bimestre, LocalDate hoy, boolean configurado) {
    String estado;
    if (bimestre.isCerrado()) {
      estado = "CERRADO";
    } else if (hoy.isBefore(bimestre.getFechaInicio())) {
      estado = "PROXIMO";
    } else if (hoy.isAfter(bimestre.getFechaFin())) {
      estado = "POR_CERRAR";
    } else {
      estado = "EN_CURSO";
    }
    return new BimestreResponse(
        bimestre.getAnhio(),
        bimestre.getNumero(),
        bimestre.getFechaInicio(),
        bimestre.getFechaFin(),
        estado,
        configurado,
        bimestre.getFechaCierre(),
        bimestre.getUsuarioCierre());
  }

  private static List<LocalDate[]> fechasPedidas(
      int anhio,
      LocalDate inicioClases,
      LocalDate finClases,
      List<BimestreFechasRequest> fechas,
      List<BimestreCalendario> existentes,
      LocalDate hoy) {
    List<LocalDate[]> nuevas = new ArrayList<>();
    if (fechas != null && !fechas.isEmpty()) {
      Set<Integer> numeros = new HashSet<>();
      fechas.forEach(fecha -> numeros.add(fecha.numero()));
      if (fechas.size() != BIMESTRES || numeros.size() != BIMESTRES) {
        throw new BusinessException("Indica las fechas de los cuatro bimestres");
      }
      fechas.stream()
          .sorted((a, b) -> a.numero().compareTo(b.numero()))
          .forEach(fecha -> nuevas.add(new LocalDate[] {fecha.fechaInicio(), fecha.fechaFin()}));
      return nuevas;
    }
    if (existentes.size() == BIMESTRES && !hoy.isBefore(existentes.get(0).getFechaInicio())) {
      existentes.forEach(
          bimestre ->
              nuevas.add(new LocalDate[] {bimestre.getFechaInicio(), bimestre.getFechaFin()}));
      nuevas.get(0)[0] = inicioClases;
      nuevas.get(BIMESTRES - 1)[1] = finClases;
      return nuevas;
    }
    proponer(anhio, inicioClases, finClases)
        .forEach(
            bimestre ->
                nuevas.add(new LocalDate[] {bimestre.getFechaInicio(), bimestre.getFechaFin()}));
    return nuevas;
  }

  private static void validarSecuencia(
      LocalDate inicioClases, LocalDate finClases, List<LocalDate[]> nuevas) {
    if (!nuevas.get(0)[0].equals(inicioClases)) {
      throw new BusinessException(
          "El bimestre 1 debe empezar con las clases, el " + inicioClases.format(FORMATO));
    }
    if (!nuevas.get(BIMESTRES - 1)[1].equals(finClases)) {
      throw new BusinessException(
          "El bimestre 4 debe terminar con las clases, el " + finClases.format(FORMATO));
    }
    for (int i = 0; i < BIMESTRES; i++) {
      if (nuevas.get(i)[1].isBefore(nuevas.get(i)[0])) {
        throw new BusinessException("El bimestre " + (i + 1) + " termina antes de empezar");
      }
      if (i > 0 && !nuevas.get(i)[0].isAfter(nuevas.get(i - 1)[1])) {
        throw new BusinessException(
            "El bimestre "
                + (i + 1)
                + " debe empezar después del "
                + nuevas.get(i - 1)[1].format(FORMATO)
                + ", cuando termina el bimestre "
                + i);
      }
    }
  }

  private void validarCambios(List<BimestreCalendario> existentes, List<LocalDate[]> nuevas) {
    LocalDate hoy = LocalDate.now(clock);
    for (int i = 0; i < BIMESTRES; i++) {
      BimestreCalendario actual = existentes.get(i);
      boolean cambiaInicio = !actual.getFechaInicio().equals(nuevas.get(i)[0]);
      boolean cambiaFin = !actual.getFechaFin().equals(nuevas.get(i)[1]);
      if (actual.isCerrado() && (cambiaInicio || cambiaFin)) {
        throw new BusinessException(
            "El bimestre "
                + actual.getNumero()
                + " está cerrado; sus fechas ya no se pueden cambiar");
      }
      if (cambiaInicio && !actual.getFechaInicio().isAfter(hoy)) {
        throw new BusinessException(
            "El bimestre "
                + actual.getNumero()
                + " ya empezó el "
                + actual.getFechaInicio().format(FORMATO)
                + "; su inicio no se puede cambiar");
      }
    }
  }

  private static LocalDate inicioDe(List<Periodo> delAnhio) {
    return delAnhio.stream().map(Periodo::getFechaInicio).min(LocalDate::compareTo).orElseThrow();
  }

  private static LocalDate finDe(List<Periodo> delAnhio) {
    return delAnhio.stream().map(Periodo::getFechaFin).max(LocalDate::compareTo).orElseThrow();
  }
}
