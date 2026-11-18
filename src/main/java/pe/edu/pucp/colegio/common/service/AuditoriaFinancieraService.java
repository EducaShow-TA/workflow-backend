package pe.edu.pucp.colegio.common.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoriaFinanciera;
import pe.edu.pucp.colegio.common.audit.AuditoriaFinanciera;
import pe.edu.pucp.colegio.common.audit.AuditoriaFinancieraRepository;
import pe.edu.pucp.colegio.common.dto.AuditoriaFinancieraResponse;
import pe.edu.pucp.colegio.common.dto.AuditoriaFinancieraResumenResponse;
import pe.edu.pucp.colegio.common.dto.FiltroAuditoriaFinanciera;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.ComprobantePago;
import pe.edu.pucp.colegio.common.model.CompromisoPago;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Registra y consulta los eventos de dinero y de estado de las boletas (HU 5.4). */
@Service
public class AuditoriaFinancieraService {

  static final String SISTEMA = "SISTEMA";
  private static final String ESTADO_COMPROMISO = "ACTIVO";
  private static final DateTimeFormatter FECHA_HORA_CSV =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final Sort ORDEN = Sort.by(Sort.Direction.DESC, "fechaHora", "id");
  private static final Map<AccionAuditoriaFinanciera, String> NOMBRES_ACCION =
      Map.of(
          AccionAuditoriaFinanciera.COMPROMISO_GENERADO, "Compromiso generado",
          AccionAuditoriaFinanciera.BOLETA_GENERADA, "Boleta generada",
          AccionAuditoriaFinanciera.PAGO_REGISTRADO, "Pago registrado",
          AccionAuditoriaFinanciera.BOLETA_EN_MORA, "Boleta en mora");
  private static final Map<String, String> NOMBRES_ESTADO =
      Map.of(
          "PENDIENTE", "Pendiente", "PAGADA", "Pagada", "EN_MORA", "En mora", "ACTIVO", "Activo");

  private final AuditoriaFinancieraRepository auditoria;
  private final CuentaUsuarioRepository usuarios;
  private final Clock clock;

  @PersistenceContext private EntityManager entityManager;

  public AuditoriaFinancieraService(
      AuditoriaFinancieraRepository auditoria, CuentaUsuarioRepository usuarios, Clock clock) {
    this.auditoria = auditoria;
    this.usuarios = usuarios;
    this.clock = clock;
  }

  @Transactional
  public void registrarCompromiso(CompromisoPago compromiso) {
    String[] actor = actorActual();
    auditoria.save(
        new AuditoriaFinanciera(
                LocalDateTime.now(clock),
                actor[0],
                actor[1],
                AccionAuditoriaFinanciera.COMPROMISO_GENERADO,
                contexto(compromiso),
                null,
                ESTADO_COMPROMISO)
            .conCuotas(compromiso.getMontoTotal(), compromiso.getNumeroCuotas()));
  }

  @Transactional
  public void registrarBoletaGenerada(BoletaPension boleta) {
    String[] actor = actorActual();
    auditoria.save(
        new AuditoriaFinanciera(
                LocalDateTime.now(clock),
                actor[0],
                actor[1],
                AccionAuditoriaFinanciera.BOLETA_GENERADA,
                contexto(boleta.getCompromiso()),
                null,
                boleta.getEstadoPago().name())
            .conBoleta(boleta.getId(), boleta.getMesCobro().atDay(1), boleta.getMonto())
            .conMecanismo(boleta.getMecanismoGeneracion().name()));
  }

  @Transactional
  public void registrarPago(
      BoletaPension boleta, EstadoBoleta estadoAnterior, ComprobantePago comprobante) {
    String[] actor = actorActual();
    auditoria.save(
        new AuditoriaFinanciera(
                LocalDateTime.now(clock),
                actor[0],
                actor[1],
                AccionAuditoriaFinanciera.PAGO_REGISTRADO,
                contexto(boleta.getCompromiso()),
                estadoAnterior.name(),
                boleta.getEstadoPago().name())
            .conBoleta(boleta.getId(), boleta.getMesCobro().atDay(1), comprobante.getMontoPagado())
            .conPago(comprobante.getMedioPago().name(), comprobante.getNumeroComprobante()));
  }

  /** La mora la marca siempre el proceso diario, aunque alguien lo dispare a mano. */
  @Transactional
  public void registrarMora(BoletaPension boleta, EstadoBoleta estadoAnterior) {
    auditoria.save(
        new AuditoriaFinanciera(
                LocalDateTime.now(clock),
                SISTEMA,
                SISTEMA,
                AccionAuditoriaFinanciera.BOLETA_EN_MORA,
                contexto(boleta.getCompromiso()),
                estadoAnterior.name(),
                boleta.getEstadoPago().name())
            .conBoleta(boleta.getId(), boleta.getMesCobro().atDay(1), boleta.calcularMontoTotal()));
  }

  @Transactional(readOnly = true)
  public Page<AuditoriaFinancieraResponse> listar(
      FiltroAuditoriaFinanciera filtro, Pageable pageable) {
    return auditoria
        .findAll(especificacion(filtro), pageable)
        .map(AuditoriaFinancieraResponse::desde);
  }

  @Transactional(readOnly = true)
  public AuditoriaFinancieraResumenResponse resumir(FiltroAuditoriaFinanciera filtro) {
    Specification<AuditoriaFinanciera> especificacion = especificacion(filtro);
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> consulta = cb.createTupleQuery();
    Root<AuditoriaFinanciera> raiz = consulta.from(AuditoriaFinanciera.class);
    Predicate condicion = especificacion.toPredicate(raiz, consulta, cb);
    consulta
        .multiselect(raiz.get("accion"), cb.count(raiz), cb.sum(raiz.<BigDecimal>get("monto")))
        .groupBy(raiz.get("accion"));
    if (condicion != null) {
      consulta.where(condicion);
    }

    Map<AccionAuditoriaFinanciera, Long> cantidades =
        new EnumMap<>(AccionAuditoriaFinanciera.class);
    BigDecimal montoPagado = BigDecimal.ZERO.setScale(2);
    for (Tuple fila : entityManager.createQuery(consulta).getResultList()) {
      AccionAuditoriaFinanciera accion = fila.get(0, AccionAuditoriaFinanciera.class);
      cantidades.put(accion, fila.get(1, Long.class));
      BigDecimal suma = fila.get(2, BigDecimal.class);
      if (accion == AccionAuditoriaFinanciera.PAGO_REGISTRADO && suma != null) {
        montoPagado = suma.setScale(2);
      }
    }
    return new AuditoriaFinancieraResumenResponse(
        cantidades.values().stream().mapToLong(Long::longValue).sum(),
        cantidades.getOrDefault(AccionAuditoriaFinanciera.PAGO_REGISTRADO, 0L),
        montoPagado,
        cantidades.getOrDefault(AccionAuditoriaFinanciera.BOLETA_EN_MORA, 0L),
        cantidades.getOrDefault(AccionAuditoriaFinanciera.BOLETA_GENERADA, 0L),
        cantidades.getOrDefault(AccionAuditoriaFinanciera.COMPROMISO_GENERADO, 0L));
  }

  /** Todos los eventos filtrados, no solo una página. Con BOM para que Excel lea las tildes. */
  @Transactional(readOnly = true)
  public String exportarCsv(FiltroAuditoriaFinanciera filtro) {
    StringBuilder csv = new StringBuilder("﻿");
    csv.append(
        "Fecha y hora,Acción,Estudiante,DNI del estudiante,Grado,Sección,Apoderado titular,"
            + "Concepto,Boleta,Mes de cobro,Estado anterior,Estado nuevo,Monto,Cuotas,Mecanismo,"
            + "Medio de pago,Comprobante,Usuario,Rol\r\n");
    for (AuditoriaFinanciera a : auditoria.findAll(especificacion(filtro), ORDEN)) {
      List<Object> celdas = new ArrayList<>();
      celdas.add(a.getFechaHora().format(FECHA_HORA_CSV));
      celdas.add(NOMBRES_ACCION.get(a.getAccion()));
      celdas.add(a.getEstudianteNombre());
      celdas.add(a.getEstudianteDni());
      celdas.add(a.getGradoNumero());
      celdas.add(a.getSeccionCodigo());
      celdas.add(a.getApoderadoNombre());
      celdas.add(a.getConcepto());
      celdas.add(a.getBoletaId());
      celdas.add(a.getMesCobro() == null ? null : a.getMesCobro().toString().substring(0, 7));
      celdas.add(nombrarEstado(a.getEstadoAnterior()));
      celdas.add(nombrarEstado(a.getEstadoNuevo()));
      celdas.add(a.getMonto());
      celdas.add(a.getNumeroCuotas());
      celdas.add(a.getMecanismo());
      celdas.add(a.getMedioPago());
      celdas.add(a.getNumeroComprobante());
      celdas.add(a.getUsuario());
      celdas.add(a.getRolUsuario());
      csv.append(String.join(",", celdas.stream().map(this::celdaCsv).toList())).append("\r\n");
    }
    return csv.toString();
  }

  Specification<AuditoriaFinanciera> especificacion(FiltroAuditoriaFinanciera filtro) {
    if (filtro.desde() != null
        && filtro.hasta() != null
        && filtro.desde().isAfter(filtro.hasta())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
    }
    return (raiz, consulta, cb) -> {
      List<Predicate> condiciones = new ArrayList<>();
      if (filtro.desde() != null) {
        condiciones.add(
            cb.greaterThanOrEqualTo(
                raiz.<LocalDateTime>get("fechaHora"), filtro.desde().atStartOfDay()));
      }
      if (filtro.hasta() != null) {
        condiciones.add(
            cb.lessThan(
                raiz.<LocalDateTime>get("fechaHora"), filtro.hasta().plusDays(1).atStartOfDay()));
      }
      if (filtro.accion() != null) {
        condiciones.add(cb.equal(raiz.get("accion"), filtro.accion()));
      }
      if (filtro.medioPago() != null) {
        condiciones.add(cb.equal(raiz.get("medioPago"), filtro.medioPago().name()));
      }
      if (filtro.gradoId() != null) {
        condiciones.add(cb.equal(raiz.get("gradoId"), filtro.gradoId()));
      }
      if (filtro.seccionId() != null) {
        condiciones.add(cb.equal(raiz.get("seccionId"), filtro.seccionId()));
      }
      if (filtro.boletaId() != null) {
        condiciones.add(cb.equal(raiz.get("boletaId"), filtro.boletaId()));
      }
      String estudiante = textoBuscado(filtro.estudiante());
      if (estudiante != null) {
        condiciones.add(
            cb.or(
                cb.like(
                    cb.lower(raiz.<String>get("estudianteNombre")), "%" + estudiante + "%", '\\'),
                cb.like(
                    cb.lower(raiz.<String>get("estudianteDni")), "%" + estudiante + "%", '\\')));
      }
      String comprobante = textoBuscado(filtro.comprobante());
      if (comprobante != null) {
        condiciones.add(
            cb.like(
                cb.lower(raiz.<String>get("numeroComprobante")), "%" + comprobante + "%", '\\'));
      }
      return cb.and(condiciones.toArray(Predicate[]::new));
    };
  }

  private String textoBuscado(String texto) {
    if (texto == null || texto.isBlank()) {
      return null;
    }
    return texto
        .trim()
        .toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  private String nombrarEstado(String estado) {
    return estado == null ? null : NOMBRES_ESTADO.getOrDefault(estado, estado);
  }

  private String celdaCsv(Object valor) {
    if (valor == null) {
      return "";
    }
    String texto = valor.toString();
    if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0) {
      texto = "'" + texto;
    }
    if (texto.contains(",") || texto.contains("\"") || texto.contains("\n")) {
      texto = "\"" + texto.replace("\"", "\"\"") + "\"";
    }
    return texto;
  }

  private AuditoriaFinanciera.Contexto contexto(CompromisoPago compromiso) {
    var matricula = compromiso.getMatricula();
    var estudiante = matricula.getEstudiante();
    var periodo = matricula.getPeriodo();
    var apoderado = compromiso.getApoderado();
    return new AuditoriaFinanciera.Contexto(
        compromiso.getId(),
        estudiante.getId(),
        recortar(estudiante.getPersona().getNombreCompleto(), 255),
        estudiante.getPersona().getDni(),
        periodo.getGrado().getId(),
        periodo.getGrado().getNumero(),
        periodo.getSeccion().getId(),
        periodo.getSeccion().getCodigo(),
        apoderado.getId(),
        recortar(apoderado.getPersona().getNombreCompleto(), 255),
        compromiso.getConcepto());
  }

  private String recortar(String texto, int maximo) {
    return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
  }

  /** Usuario y rol de quien hizo la operación; sin sesión, el proceso automático. */
  private String[] actorActual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return new String[] {SISTEMA, SISTEMA};
    }
    String rol =
        usuarios
            .findByUsername(auth.getName())
            .map(usuario -> usuario.getRol().name())
            .orElseGet(
                () ->
                    auth.getAuthorities().stream()
                        .findFirst()
                        .map(Object::toString)
                        .orElse(SISTEMA)
                        .replace("ROLE_", ""));
    return new String[] {recortar(auth.getName(), 100), recortar(rol, 30)};
  }
}
