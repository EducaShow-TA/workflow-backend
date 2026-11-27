package pe.edu.pucp.colegio.common.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.audit.AccionAuditoriaFinanciera;
import pe.edu.pucp.colegio.common.audit.AccionesCalificacion;
import pe.edu.pucp.colegio.common.audit.AuditoriaCalificacion;
import pe.edu.pucp.colegio.common.audit.AuditoriaFinanciera;
import pe.edu.pucp.colegio.common.audit.NivelLog;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse.Actividad;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse.CobranzaMes;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse.Indicadores;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse.OcupacionGrado;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse.Pendientes;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse.VentanaMatricula;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaResponse;
import pe.edu.pucp.colegio.common.dto.VacantesPeriodoResponse;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.repository.BoletaPensionFiltros;
import pe.edu.pucp.colegio.common.repository.BoletaPensionRepository;

/** Resumen del colegio para el panel del administrador: pendientes, indicadores y actividad. */
@Service
public class PanelAdministradorService {

  private static final int LIMITE_ACTIVIDAD = 6;
  private static final String BOLETA_VIGENTE =
      " and b.compromiso.matricula.estado <> pe.edu.pucp.colegio.common.model.EstadoMatricula.RETIRADA";
  private static final Locale ES = Locale.forLanguageTag("es-PE");

  @PersistenceContext private EntityManager em;

  private final Clock clock;
  private final VacantesService vacantes;
  private final BoletaPensionRepository boletas;
  private final PeriodoMatriculaService periodosMatricula;

  public PanelAdministradorService(
      Clock clock,
      VacantesService vacantes,
      BoletaPensionRepository boletas,
      PeriodoMatriculaService periodosMatricula) {
    this.clock = clock;
    this.vacantes = vacantes;
    this.boletas = boletas;
    this.periodosMatricula = periodosMatricula;
  }

  @Transactional(readOnly = true)
  public PanelAdministradorResponse resumen() {
    LocalDate hoy = LocalDate.now(clock);
    int anio = hoy.getYear();
    List<OcupacionGrado> ocupacion = ocupacion(anio);
    Map<EstadoAsistenciaDocente, Long> docentesHoy = docentesHoy(hoy);
    long presentesHoy =
        docentesHoy.getOrDefault(EstadoAsistenciaDocente.PRESENTE, 0L)
            + docentesHoy.getOrDefault(EstadoAsistenciaDocente.TARDANZA, 0L);
    long conClaseHoy = docentesConClase(hoy, anio);
    Map<EstadoAsistencia, Long> semana = asistenciaSemana(hoy);
    long presentesSemana = semana.getOrDefault(EstadoAsistencia.PRESENTE, 0L);
    long tardanzasSemana = semana.getOrDefault(EstadoAsistencia.TARDANZA, 0L);
    long ausentesSemana = semana.getOrDefault(EstadoAsistencia.AUSENTE, 0L);
    var mora = BoletaPensionFiltros.reporte(null, null, EstadoBoleta.EN_MORA, null, null, true);
    LocalDateTime hace24h = LocalDateTime.now(clock).minusHours(24);

    return new PanelAdministradorResponse(
        hoy,
        anio,
        ventanaMatricula(),
        new Pendientes(
            contar(
                "select count(m) from Matricula m where m.active = true and m.estado = :estado",
                Map.of("estado", EstadoMatricula.EN_REGISTRO)),
            anhiosPorConfirmar(),
            boletas.contarFamiliasMorosas(mora),
            boletas.sumarPendiente(mora),
            Math.max(0, conClaseHoy - presentesHoy),
            contar(
                "select count(r) from RegistroError r where r.fechaHora >= :desde and r.nivel in :niveles",
                Map.of("desde", hace24h, "niveles", List.of(NivelLog.ERROR, NivelLog.CRITICO))),
            contar(
                "select count(r) from RegistroError r where r.fechaHora >= :desde and r.nivel = :nivel",
                Map.of("desde", hace24h, "nivel", NivelLog.CRITICO))),
        new Indicadores(
            ocupacion.stream().mapToLong(OcupacionGrado::matriculados).sum(),
            ocupacion.stream().mapToLong(grado -> grado.matriculados() - grado.pendientes()).sum(),
            ocupacion.stream().mapToLong(OcupacionGrado::vacantes).sum(),
            pensionDelMes(hoy, false),
            pensionDelMes(hoy, true),
            presentesSemana + tardanzasSemana + ausentesSemana,
            presentesSemana + tardanzasSemana,
            tardanzasSemana,
            conClaseHoy,
            presentesHoy,
            docentesHoy.getOrDefault(EstadoAsistenciaDocente.TARDANZA, 0L)),
        cobranza(hoy),
        ocupacion,
        actividad());
  }

  private VentanaMatricula ventanaMatricula() {
    return periodosMatricula.listar().stream()
        .max(
            Comparator.comparingInt(PeriodoMatriculaResponse::anhioEscolar)
                .thenComparing(PeriodoMatriculaResponse::fechaInicio))
        .map(
            periodo ->
                new VentanaMatricula(
                    periodo.fechaInicio(),
                    periodo.fechaLimite(),
                    periodo.estado(),
                    periodo.anhioEscolar()))
        .orElse(null);
  }

  private List<Integer> anhiosPorConfirmar() {
    return em
        .createQuery(
            "select distinct extract(year from m.periodo.fechaInicio) from Matricula m"
                + " where m.active = true and m.estado = :estado",
            Integer.class)
        .setParameter("estado", EstadoMatricula.EN_REGISTRO)
        .getResultList()
        .stream()
        .sorted()
        .toList();
  }

  private List<OcupacionGrado> ocupacion(int anio) {
    Map<Integer, VacantesPeriodoResponse> porSeccion = new LinkedHashMap<>();
    for (VacantesPeriodoResponse v : vacantes.listar(anio, null, null)) {
      porSeccion.putIfAbsent(v.seccionId(), v);
    }
    Map<String, OcupacionGrado> porGrado = new TreeMap<>();
    for (VacantesPeriodoResponse v : porSeccion.values()) {
      String clave = v.nivelEducativo() + ":" + v.numeroGrado();
      porGrado.merge(
          clave,
          new OcupacionGrado(
              v.nivelEducativo(),
              v.numeroGrado(),
              1,
              v.matriculasActivas(),
              v.vacantesTotales(),
              v.matriculasPendientes()),
          (a, b) ->
              new OcupacionGrado(
                  a.nivelEducativo(),
                  a.grado(),
                  a.secciones() + b.secciones(),
                  a.matriculados() + b.matriculados(),
                  a.vacantes() + b.vacantes(),
                  a.pendientes() + b.pendientes()));
    }
    return porGrado.values().stream()
        .sorted(
            Comparator.comparing(OcupacionGrado::nivelEducativo)
                .thenComparingInt(OcupacionGrado::grado))
        .toList();
  }

  private Map<EstadoAsistenciaDocente, Long> docentesHoy(LocalDate hoy) {
    Map<EstadoAsistenciaDocente, Long> conteo = new EnumMap<>(EstadoAsistenciaDocente.class);
    em.createQuery(
            "select a.estado, count(a) from AsistenciaDocente a"
                + " where a.active = true and a.fecha = :hoy group by a.estado",
            Object[].class)
        .setParameter("hoy", hoy)
        .getResultList()
        .forEach(f -> conteo.put((EstadoAsistenciaDocente) f[0], (Long) f[1]));
    return conteo;
  }

  private long docentesConClase(LocalDate hoy, int anio) {
    boolean noLectivo =
        contar(
                "select count(d) from DiaNoLectivo d where d.active = true and d.fecha = :hoy",
                Map.of("hoy", hoy))
            > 0;
    if (noLectivo) {
      return 0;
    }
    return contar(
        "select count(distinct s.docente.id) from SesionHorario s"
            + " where s.docente is not null and s.dia = :dia and s.horario.active = true"
            + " and s.horario.anio = :anio"
            + " and (s.vigenteDesde is null or s.vigenteDesde <= :hoy)"
            + " and (s.vigenteHasta is null or s.vigenteHasta >= :hoy)",
        Map.of("dia", hoy.getDayOfWeek(), "anio", anio, "hoy", hoy));
  }

  private Map<EstadoAsistencia, Long> asistenciaSemana(LocalDate hoy) {
    Map<EstadoAsistencia, Long> conteo = new EnumMap<>(EstadoAsistencia.class);
    em.createQuery(
            "select a.estadoAsistencia, count(a) from AsistenciaEstudiante a"
                + " where a.active = true and a.sesionClase.active = true"
                + " and a.sesionClase.cancelada = false"
                + " and a.sesionClase.fecha between :desde and :hasta"
                + " group by a.estadoAsistencia",
            Object[].class)
        .setParameter("desde", hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
        .setParameter("hasta", hoy)
        .getResultList()
        .forEach(f -> conteo.put((EstadoAsistencia) f[0], (Long) f[1]));
    return conteo;
  }

  private BigDecimal pensionDelMes(LocalDate hoy, boolean soloPagadas) {
    var consulta =
        em.createQuery(
                "select sum(b.monto) from BoletaPension b where b.active = true"
                    + BOLETA_VIGENTE
                    + " and b.mesCobro between :desde and :hasta"
                    + (soloPagadas ? " and b.estadoPago = :pagada" : ""),
                BigDecimal.class)
            .setParameter("desde", hoy.withDayOfMonth(1))
            .setParameter("hasta", hoy.with(TemporalAdjusters.lastDayOfMonth()));
    if (soloPagadas) {
      consulta.setParameter("pagada", EstadoBoleta.PAGADA);
    }
    BigDecimal total = consulta.getSingleResult();
    return total == null ? BigDecimal.ZERO : total;
  }

  private List<CobranzaMes> cobranza(LocalDate hoy) {
    Map<Integer, BigDecimal[]> meses = new TreeMap<>();
    acumular(meses, 0, "b.estadoPago = :pagada", Map.of("pagada", EstadoBoleta.PAGADA), hoy);
    acumular(
        meses,
        1,
        "(b.estadoPago = :mora or (b.estadoPago = :pendiente and b.fechaVencimiento < :hoy))",
        Map.of("mora", EstadoBoleta.EN_MORA, "pendiente", EstadoBoleta.PENDIENTE, "hoy", hoy),
        hoy);
    acumular(
        meses,
        2,
        "b.estadoPago = :pendiente and b.fechaVencimiento >= :hoy",
        Map.of("pendiente", EstadoBoleta.PENDIENTE, "hoy", hoy),
        hoy);
    return meses.entrySet().stream()
        .map(e -> new CobranzaMes(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
        .toList();
  }

  private void acumular(
      Map<Integer, BigDecimal[]> meses,
      int posicion,
      String condicion,
      Map<String, ?> parametros,
      LocalDate hoy) {
    var consulta =
        em.createQuery(
            "select month(b.mesCobro), sum(b.monto) from BoletaPension b where b.active = true"
                + BOLETA_VIGENTE
                + " and b.mesCobro between :inicio and :fin and "
                + condicion
                + " group by month(b.mesCobro)",
            Object[].class);
    consulta.setParameter("inicio", LocalDate.of(hoy.getYear(), 1, 1));
    consulta.setParameter("fin", LocalDate.of(hoy.getYear(), 12, 31));
    for (Map.Entry<String, ?> parametro : parametros.entrySet()) {
      consulta.setParameter(parametro.getKey(), parametro.getValue());
    }
    for (Object[] fila : consulta.getResultList()) {
      BigDecimal[] montos =
          meses.computeIfAbsent(
              ((Number) fila[0]).intValue(),
              m -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
      montos[posicion] = fila[1] == null ? BigDecimal.ZERO : (BigDecimal) fila[1];
    }
  }

  private List<Actividad> actividad() {
    List<Actividad> lista = new ArrayList<>();
    em.createQuery(
            "select a from AuditoriaFinanciera a where a.accion in :acciones"
                + " order by a.fechaHora desc, a.id desc",
            AuditoriaFinanciera.class)
        .setParameter(
            "acciones",
            List.of(
                AccionAuditoriaFinanciera.PAGO_REGISTRADO,
                AccionAuditoriaFinanciera.BOLETA_EN_MORA))
        .setMaxResults(LIMITE_ACTIVIDAD)
        .getResultList()
        .forEach(a -> lista.add(actividadFinanciera(a)));
    em.createQuery(
            "select a from AuditoriaCalificacion a where a.accion = :accion"
                + " order by a.fechaHora desc, a.id desc",
            AuditoriaCalificacion.class)
        .setParameter("accion", AccionesCalificacion.RECTIFICAR)
        .setMaxResults(LIMITE_ACTIVIDAD)
        .getResultList()
        .forEach(
            a ->
                lista.add(
                    new Actividad(
                        "RECTIFICACION",
                        "Calificación rectificada",
                        "Competencia "
                            + a.getCompetenciaCodigo()
                            + " · por "
                            + a.getUsuario()
                            + (a.getMotivo() == null ? "" : " · " + a.getMotivo()),
                        a.getFechaHora())));
    return lista.stream()
        .sorted(
            Comparator.comparing(
                Actividad::fecha, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
        .limit(LIMITE_ACTIVIDAD)
        .toList();
  }

  private static Actividad actividadFinanciera(AuditoriaFinanciera a) {
    boolean pago = a.getAccion() == AccionAuditoriaFinanciera.PAGO_REGISTRADO;
    String mes =
        a.getMesCobro() == null
            ? ""
            : capitalizar(a.getMesCobro().getMonth().getDisplayName(TextStyle.FULL, ES)) + " · ";
    return new Actividad(
        pago ? "PAGO" : "MORA",
        pago ? "Pago de pensión registrado" : "Boleta pasó a mora",
        mes
            + a.getEstudianteNombre()
            + (pago && a.getMonto() != null
                ? " · S/ " + a.getMonto().setScale(2, RoundingMode.HALF_UP)
                : ""),
        a.getFechaHora());
  }

  private static String capitalizar(String texto) {
    return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
  }

  private long contar(String jpql, Map<String, ?> parametros) {
    var consulta = em.createQuery(jpql, Long.class);
    for (Map.Entry<String, ?> parametro : parametros.entrySet()) {
      consulta.setParameter(parametro.getKey(), parametro.getValue());
    }
    Long total = consulta.getSingleResult();
    return total == null ? 0 : total;
  }
}
