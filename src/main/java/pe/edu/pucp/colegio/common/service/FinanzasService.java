package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.dto.BoletaPensionReporteResponse;
import pe.edu.pucp.colegio.common.dto.BoletaPensionResponse;
import pe.edu.pucp.colegio.common.dto.ComprobantePagoResponse;
import pe.edu.pucp.colegio.common.dto.CompromisoPagoRequest;
import pe.edu.pucp.colegio.common.dto.CompromisoPagoResponse;
import pe.edu.pucp.colegio.common.dto.EstudianteConBoletasResponse;
import pe.edu.pucp.colegio.common.dto.PaginaBoletasResponse;
import pe.edu.pucp.colegio.common.dto.RegistrarPagoRequest;
import pe.edu.pucp.colegio.common.dto.ReporteBoletasPaginadoResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.ComprobantePago;
import pe.edu.pucp.colegio.common.model.CompromisoPago;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.MecanismoGeneracion;
import pe.edu.pucp.colegio.common.model.TipoCargoBoleta;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;
import pe.edu.pucp.colegio.common.repository.BoletaPensionFiltros;
import pe.edu.pucp.colegio.common.repository.BoletaPensionRepository;
import pe.edu.pucp.colegio.common.repository.ComprobantePagoRepository;
import pe.edu.pucp.colegio.common.repository.CompromisoPagoRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Compromisos, cronograma de pensiones, comprobantes, mora y cobranza consolidada. */
@Service
public class FinanzasService {
  private static final Logger log = LoggerFactory.getLogger(FinanzasService.class);
  private static final String COMPROMISO_EXISTENTE =
      "La matrícula ya tiene un compromiso de pago activo.";
  private static final String APODERADO_PRINCIPAL_FALTANTE =
      "El estudiante debe tener un apoderado principal activo para generar el compromiso.";
  private static final Sort ORDEN_BOLETAS =
      Sort.by("fechaVencimiento").ascending().and(Sort.by("id").ascending());
  private final CompromisoPagoRepository compromisos;
  private final BoletaPensionRepository boletas;
  private final ComprobantePagoRepository comprobantes;
  private final VinculoFamiliarRepository vinculos;
  private final RegistroAuditoriaRepository auditoria;
  private final CuentaUsuarioRepository usuarios;
  private final Clock clock;
  private final TransactionTemplate transaccionPorCompromiso;
  private final AuditoriaFinancieraService auditoriaFinanciera;

  public FinanzasService(
      CompromisoPagoRepository compromisos,
      BoletaPensionRepository boletas,
      ComprobantePagoRepository comprobantes,
      VinculoFamiliarRepository vinculos,
      RegistroAuditoriaRepository auditoria,
      CuentaUsuarioRepository usuarios,
      Clock clock,
      PlatformTransactionManager transactionManager,
      AuditoriaFinancieraService auditoriaFinanciera) {
    this.compromisos = compromisos;
    this.boletas = boletas;
    this.comprobantes = comprobantes;
    this.vinculos = vinculos;
    this.auditoria = auditoria;
    this.usuarios = usuarios;
    this.clock = clock;
    this.transaccionPorCompromiso = new TransactionTemplate(transactionManager);
    this.transaccionPorCompromiso.setPropagationBehavior(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.auditoriaFinanciera = auditoriaFinanciera;
  }

  @Transactional
  public CompromisoPagoResponse crearCompromiso(
      Matricula matricula,
      CompromisoPagoRequest request,
      BigDecimal montoMatricula,
      BigDecimal montoPensionMensual) {
    if (montoMatricula == null
        || montoMatricula.signum() <= 0
        || montoPensionMensual == null
        || montoPensionMensual.signum() <= 0) {
      throw new BusinessException(
          "Las tarifas anuales de matrícula y pensión no están configuradas");
    }
    BigDecimal totalPensiones =
        montoPensionMensual
            .multiply(BigDecimal.valueOf(request.numeroCuotas()))
            .setScale(2, RoundingMode.HALF_UP);
    CompromisoPagoRequest terminosPensiones =
        new CompromisoPagoRequest(
            request.concepto(),
            totalPensiones,
            request.numeroCuotas(),
            request.primerVencimiento(),
            request.diaVencimiento());
    CompromisoPagoResponse respuesta = crearCompromiso(matricula, terminosPensiones);
    CompromisoPago compromiso =
        compromisos
            .findByMatriculaId(matricula.getId())
            .orElseThrow(
                () -> new IllegalStateException("No se guardó el compromiso de pensiones"));
    generarBoletaMatricula(compromiso, montoMatricula, request.primerVencimiento());
    return respuesta;
  }

  @Transactional
  public CompromisoPagoResponse crearCompromiso(
      Matricula matricula, CompromisoPagoRequest request) {
    validarTerminos(request);
    List<String> requisitos = validarRequisitosCompromiso(matricula);
    if (requisitos.contains(COMPROMISO_EXISTENTE)) {
      throw new ConflictException("La matrícula ya tiene un compromiso de pago");
    }
    if (!requisitos.isEmpty()) {
      throw new BusinessException(
          "El estudiante debe tener un apoderado principal para generar el compromiso");
    }
    VinculoFamiliar vinculo =
        vinculos
            .findFirstByEstudianteIdAndEsPrincipalTrueAndActiveTrueOrderByIdAsc(
                matricula.getEstudiante().getId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        "El estudiante debe tener un apoderado principal para generar el compromiso"));
    CompromisoPago existente = compromisos.findByMatriculaId(matricula.getId()).orElse(null);
    if (existente != null) {
      existente.setActive(true);
      existente.setApoderado(vinculo.getApoderado());
      existente.setConcepto(request.concepto().trim());
      existente.setMontoTotal(request.montoTotal().setScale(2, RoundingMode.HALF_UP));
      existente.setFechaEmision(LocalDate.now(clock));
      existente.setMesInicio(YearMonth.from(request.primerVencimiento()));
      existente.setNumeroCuotas(request.numeroCuotas());
      existente.setPrimerVencimiento(request.primerVencimiento());
      existente.setDiaVencimiento(request.diaVencimiento());
      existente = compromisos.save(existente);
      auditoriaFinanciera.registrarCompromiso(existente);
      LocalDate primerMes = YearMonth.from(existente.getPrimerVencimiento()).atDay(1);
      boolean yaTieneBoletaDelCompromiso =
          boletas.existsByCompromisoIdAndMesCobroAndTipoCargoAndActiveTrue(
              existente.getId(), primerMes, TipoCargoBoleta.PENSION);
      boolean yaTieneBoletaDelEstudiante =
          boletas.existsByEstudianteConceptoMes(
              matricula.getEstudiante().getId(), existente.getConcepto(), primerMes);
      if (!yaTieneBoletaDelCompromiso && !yaTieneBoletaDelEstudiante) {
        generarCronogramaCompleto(existente);
      } else {
        generarCronogramaCompleto(existente);
      }
      return respuesta(existente);
    }
    CompromisoPago c = new CompromisoPago();
    c.setMatricula(matricula);
    c.setApoderado(vinculo.getApoderado());
    c.setConcepto(request.concepto().trim());
    c.setMontoTotal(request.montoTotal().setScale(2, RoundingMode.HALF_UP));
    c.setFechaEmision(LocalDate.now(clock));
    c.setMesInicio(YearMonth.from(request.primerVencimiento()));
    c.setNumeroCuotas(request.numeroCuotas());
    c.setPrimerVencimiento(request.primerVencimiento());
    c.setDiaVencimiento(request.diaVencimiento());
    c = compromisos.save(c);
    auditoriaFinanciera.registrarCompromiso(c);
    generarCronogramaCompleto(c);
    return respuesta(c);
  }

  private void generarCronogramaCompleto(CompromisoPago compromiso) {
    for (int indice = 0; indice < compromiso.getNumeroCuotas(); indice++) {
      YearMonth mes = compromiso.getMesInicio().plusMonths(indice);
      if (!boletas.existsByCompromisoIdAndMesCobroAndTipoCargoAndActiveTrue(
          compromiso.getId(), mes.atDay(1), TipoCargoBoleta.PENSION)) {
        generarBoleta(compromiso, mes, MecanismoGeneracion.AUTOMATICO);
      }
    }
  }

  /** Precondiciones por estudiante para que una confirmación masiva pueda crear el compromiso. */
  @Transactional(readOnly = true)
  public List<String> validarRequisitosCompromiso(Matricula matricula) {
    List<String> problemas = new ArrayList<>();
    if (compromisos.findByMatriculaIdAndActiveTrue(matricula.getId()).isPresent()) {
      problemas.add(COMPROMISO_EXISTENTE);
    }
    boolean tieneApoderadoPrincipal =
        vinculos
            .findFirstByEstudianteIdAndEsPrincipalTrueAndActiveTrueOrderByIdAsc(
                matricula.getEstudiante().getId())
            .filter(vinculo -> Boolean.TRUE.equals(vinculo.getApoderado().getActive()))
            .isPresent();
    if (!tieneApoderadoPrincipal) {
      problemas.add(APODERADO_PRINCIPAL_FALTANTE);
    }
    return List.copyOf(problemas);
  }

  @Transactional(readOnly = true)
  public List<CompromisoPagoResponse> listarCompromisos(Integer estudianteId) {
    return compromisos.findAllByActiveTrue().stream()
        .filter(
            c ->
                estudianteId == null
                    || c.getMatricula().getEstudiante().getId().equals(estudianteId))
        .map(this::respuesta)
        .toList();
  }

  @Transactional(readOnly = true)
  public CompromisoPagoResponse obtenerCompromiso(Integer id) {
    return respuesta(buscarCompromiso(id));
  }

  @Transactional
  public void desactivarCompromisoDeMatricula(Integer matriculaId) {
    compromisos
        .findByMatriculaIdAndActiveTrue(matriculaId)
        .ifPresent(compromiso -> compromiso.setActive(false));
  }

  @Transactional
  public BoletaPensionResponse generarManual(Integer matriculaId, YearMonth mes) {
    CompromisoPago c =
        compromisos
            .findByMatriculaIdAndActiveTrue(matriculaId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el compromiso de la matrícula"));
    validarMesCompromiso(c, mes);
    return respuesta(generarBoleta(c, mes, MecanismoGeneracion.MANUAL));
  }

  @Transactional(readOnly = true)
  public List<BoletaPensionResponse> listarBoletas(
      Integer estudianteId, Integer periodoId, YearMonth mes, EstadoBoleta estado) {
    return boletas
        .findAll(BoletaPensionFiltros.listado(estudianteId, periodoId, mes, estado), ORDEN_BOLETAS)
        .stream()
        .map(this::respuesta)
        .toList();
  }

  @Transactional(readOnly = true)
  public BoletaPensionResponse obtenerBoleta(Integer id) {
    return respuesta(buscarBoleta(id));
  }

  /**
   * Un pago administrativo registrado se considera aprobado; no representa una integración externa.
   */
  @Transactional
  public ComprobantePagoResponse registrarPago(Integer boletaId, RegistrarPagoRequest request) {
    BoletaPension boleta = buscarBoletaParaPago(boletaId);
    if (boleta.getEstadoPago() == EstadoBoleta.PAGADA
        || comprobantes.existsByBoletaIdAndActiveTrue(boletaId)) {
      throw new ConflictException("La boleta ya tiene un pago registrado");
    }
    BigDecimal importe = boleta.calcularMontoTotal().setScale(2, RoundingMode.HALF_UP);
    if (request.monto().setScale(2, RoundingMode.HALF_UP).compareTo(importe) != 0) {
      throw new BusinessException(
          "El monto del pago debe coincidir con el total de la boleta: " + importe);
    }
    ComprobantePago comprobante = new ComprobantePago();
    comprobante.setBoleta(boleta);
    comprobante.setFechaEmision(LocalDate.now(clock));
    comprobante.setMontoPagado(importe);
    comprobante.setMedioPago(request.medioPago());
    String numeroComprobante =
        request.referencia() == null || request.referencia().isBlank()
            ? "PEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
            : request.referencia().trim();
    if (comprobantes.existsByNumeroComprobante(numeroComprobante)) {
      throw new ConflictException("El número de comprobante ya está registrado");
    }
    comprobante.setNumeroComprobante(numeroComprobante);
    comprobante = comprobantes.save(comprobante);
    EstadoBoleta estadoAnterior = boleta.getEstadoPago();
    boleta.setEstadoPago(EstadoBoleta.PAGADA);
    boletas.save(boleta);
    auditoriaFinanciera.registrarPago(boleta, estadoAnterior, comprobante);
    registrarAuditoria(
        AccionAuditoria.REGISTRAR_PAGO,
        "Comprobante "
            + comprobante.getNumeroComprobante()
            + " aprobado para boleta "
            + boletaId
            + " por "
            + request.medioPago());
    return respuesta(comprobante);
  }

  @Transactional(readOnly = true)
  public List<ComprobantePagoResponse> comprobantes(Integer boletaId) {
    buscarBoleta(boletaId);
    return comprobantes.findAllByBoletaIdAndActiveTrueOrderByFechaEmisionDesc(boletaId).stream()
        .map(this::respuesta)
        .toList();
  }

  @Transactional(readOnly = true)
  public BoletaPensionReporteResponse reporte(
      Integer gradoId,
      Integer seccionId,
      EstadoBoleta estado,
      LocalDate vencimientoDesde,
      LocalDate vencimientoHasta) {
    validarVencimientos(vencimientoDesde, vencimientoHasta);
    var filtro =
        BoletaPensionFiltros.reporte(
            gradoId, seccionId, estado, vencimientoDesde, vencimientoHasta, false);
    var resultado = boletas.findAll(filtro, ORDEN_BOLETAS);
    return new BoletaPensionReporteResponse(
        resultado.stream().map(this::respuesta).toList(),
        boletas.sumarPendiente(filtro),
        boletas.contarFamiliasMorosas(filtro));
  }

  @Transactional(readOnly = true)
  public PaginaBoletasResponse paginaBoletas(
      Integer estudianteId,
      Integer periodoId,
      YearMonth mes,
      EstadoBoleta estado,
      int page,
      int size) {
    return consultarPagina(
        BoletaPensionFiltros.listado(estudianteId, periodoId, mes, estado), page, size);
  }

  @Transactional(readOnly = true)
  public ReporteBoletasPaginadoResponse paginaReporte(
      Integer gradoId,
      Integer seccionId,
      EstadoBoleta estado,
      LocalDate desde,
      LocalDate hasta,
      int page,
      int size) {
    validarVencimientos(desde, hasta);
    if (estado == EstadoBoleta.PAGADA)
      throw new BusinessException("El reporte de cobranza solo admite pendientes y en mora");
    var filtro = BoletaPensionFiltros.reporte(gradoId, seccionId, estado, desde, hasta, true);
    return new ReporteBoletasPaginadoResponse(
        consultarPagina(filtro, page, size),
        boletas.sumarPendiente(filtro),
        boletas.contarFamiliasMorosas(filtro));
  }

  @Transactional(readOnly = true)
  public List<EstudianteConBoletasResponse> estudiantesConBoletas() {
    return boletas.listarEstudiantesConBoletas();
  }

  private PaginaBoletasResponse consultarPagina(
      Specification<BoletaPension> filtro, int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Paginación inválida: page >= 0 y size entre 1 y 100");
    }
    return PaginaBoletasResponse.desde(
        boletas.findAll(filtro, PageRequest.of(page, size, ORDEN_BOLETAS)).map(this::respuesta));
  }

  private void validarVencimientos(LocalDate desde, LocalDate hasta) {
    if (desde != null && hasta != null && hasta.isBefore(desde)) {
      throw new BusinessException(
          "El fin del rango de vencimiento debe ser igual o posterior al inicio");
    }
  }

  /** Generación idempotente del mes actual y actualización diaria de vencimientos. */
  @Scheduled(cron = "0 10 1 * * *", zone = "America/Lima")
  public void procesarCicloDiario() {
    YearMonth mesActual = YearMonth.now(clock);
    for (Integer compromisoId : compromisos.findAllActiveIds()) {
      try {
        transaccionPorCompromiso.executeWithoutResult(
            status ->
                compromisos
                    .findById(compromisoId)
                    .filter(c -> Boolean.TRUE.equals(c.getActive()))
                    .filter(c -> c.getMatricula().getEstado() != EstadoMatricula.RETIRADA)
                    .ifPresent(c -> generarBoletasPendientes(c, mesActual)));
      } catch (RuntimeException ex) {
        log.error(
            "No se pudo procesar el compromiso de pago {} en el ciclo diario", compromisoId, ex);
      }
    }
    LocalDate hoy = LocalDate.now(clock);
    transaccionPorCompromiso.executeWithoutResult(
        status -> {
          for (BoletaPension b :
              boletas.findAllByActiveTrueAndEstadoPagoInAndFechaVencimientoBefore(
                  List.of(EstadoBoleta.PENDIENTE), hoy)) {
            marcarSiVencida(b, hoy);
          }
        });
  }

  /** Lo que se emitió y lo que pasó a mora al completar las boletas de un estudiante. */
  public record BoletasCompletadas(YearMonth mesCorte, int generadas, int enMora) {}

  /**
   * Emite las boletas que falten hasta el mes actual y marca la mora de un solo estudiante. A
   * diferencia del ciclo diario, usa una única transacción: si una boleta falla no se guarda
   * ninguna y el administrador puede reintentar.
   */
  @Transactional
  public BoletasCompletadas completarBoletasDeEstudiante(Integer estudianteId) {
    YearMonth mesActual = YearMonth.now(clock);
    LocalDate hoy = LocalDate.now(clock);
    int generadas = 0;
    for (CompromisoPago c : compromisos.findAllByMatriculaEstudianteId(estudianteId)) {
      if (Boolean.TRUE.equals(c.getActive())
          && c.getMatricula().getEstado() != EstadoMatricula.RETIRADA) {
        generadas += generarBoletasPendientes(c, mesActual);
      }
    }
    int enMora = 0;
    for (BoletaPension b :
        boletas.findAllByCompromisoMatriculaEstudianteIdAndActiveTrueOrderByMesCobroAsc(
            estudianteId)) {
      if (marcarSiVencida(b, hoy)) {
        enMora++;
      }
    }
    return new BoletasCompletadas(mesActual, generadas, enMora);
  }

  private boolean marcarSiVencida(BoletaPension b, LocalDate hoy) {
    if (b.getCompromiso().getMatricula().getEstado() == EstadoMatricula.RETIRADA) {
      return false;
    }
    EstadoBoleta estadoAnterior = b.getEstadoPago();
    b.marcarEnMora(hoy);
    if (b.getEstadoPago() == estadoAnterior) {
      return false;
    }
    auditoriaFinanciera.registrarMora(b, estadoAnterior);
    return true;
  }

  private int generarBoletasPendientes(CompromisoPago c, YearMonth mesActual) {
    int generadas = 0;
    YearMonth ultimoMes =
        mesActual.isBefore(c.getMesInicio().plusMonths(c.getNumeroCuotas() - 1L))
            ? mesActual
            : c.getMesInicio().plusMonths(c.getNumeroCuotas() - 1L);
    for (YearMonth mes = c.getMesInicio(); !mes.isAfter(ultimoMes); mes = mes.plusMonths(1)) {
      if (!boletas.existsByEstudianteConceptoMes(
          c.getMatricula().getEstudiante().getId(), c.getConcepto(), mes.atDay(1))) {
        generarBoleta(c, mes, MecanismoGeneracion.AUTOMATICO);
        generadas++;
      }
    }
    return generadas;
  }

  private BoletaPension generarBoleta(
      CompromisoPago c, YearMonth mes, MecanismoGeneracion mecanismo) {
    validarMesCompromiso(c, mes);
    if (boletas.existsByCompromisoIdAndMesCobroAndTipoCargoAndActiveTrue(
        c.getId(), mes.atDay(1), TipoCargoBoleta.PENSION)) {
      throw new ConflictException("Ya existe una boleta para el compromiso y mes indicados");
    }
    if (boletas.existsByEstudianteConceptoMes(
        c.getMatricula().getEstudiante().getId(), c.getConcepto(), mes.atDay(1))) {
      throw new ConflictException(
          "Ya existe una boleta para el estudiante, concepto y mes indicados");
    }
    int indice = Math.toIntExact(ChronoUnit.MONTHS.between(c.getMesInicio(), mes)) + 1;
    BoletaPension b = new BoletaPension();
    b.setCompromiso(c);
    b.setTipoCargo(TipoCargoBoleta.PENSION);
    b.setMesCobro(mes);
    b.setMonto(c.getMontoCuota(indice));
    b.setFechaVencimiento(
        indice == 1 ? c.getPrimerVencimiento() : mes.atDay(c.getDiaVencimiento()));
    b.setMora(BigDecimal.ZERO.setScale(2));
    b.setEstadoPago(EstadoBoleta.PENDIENTE);
    b.setMecanismoGeneracion(mecanismo);
    b.setFechaGeneracion(LocalDateTime.now(clock));
    b = boletas.save(b);
    auditoriaFinanciera.registrarBoletaGenerada(b);
    registrarAuditoria(
        AccionAuditoria.GENERAR_BOLETA,
        "Boleta " + b.getId() + " generada " + mecanismo + " para " + mes);
    return b;
  }

  private BoletaPension generarBoletaMatricula(
      CompromisoPago compromiso, BigDecimal monto, LocalDate vencimiento) {
    YearMonth mes = YearMonth.from(vencimiento);
    if (boletas.existsByCompromisoIdAndTipoCargoAndActiveTrue(
        compromiso.getId(), TipoCargoBoleta.MATRICULA)) {
      return boletas
          .findAllByCompromisoIdAndActiveTrueOrderByMesCobroAsc(compromiso.getId())
          .stream()
          .filter(boleta -> boleta.getTipoCargo() == TipoCargoBoleta.MATRICULA)
          .findFirst()
          .orElseThrow(
              () ->
                  new IllegalStateException("Existe un cargo de matrícula sin poder consultarlo"));
    }
    BoletaPension boleta = new BoletaPension();
    boleta.setCompromiso(compromiso);
    boleta.setTipoCargo(TipoCargoBoleta.MATRICULA);
    boleta.setMesCobro(mes);
    boleta.setMonto(monto.setScale(2, RoundingMode.HALF_UP));
    boleta.setFechaVencimiento(vencimiento);
    boleta.setMora(BigDecimal.ZERO.setScale(2));
    boleta.setEstadoPago(EstadoBoleta.PENDIENTE);
    boleta.setMecanismoGeneracion(MecanismoGeneracion.AUTOMATICO);
    boleta.setFechaGeneracion(LocalDateTime.now(clock));
    boleta = boletas.save(boleta);
    auditoriaFinanciera.registrarBoletaGenerada(boleta);
    registrarAuditoria(
        AccionAuditoria.GENERAR_BOLETA,
        "Boleta de matrícula " + boleta.getId() + " generada para " + mes);
    return boleta;
  }

  public void validarTerminos(CompromisoPagoRequest r) {
    if (r.numeroCuotas() > 24)
      throw new BusinessException("El cronograma no puede superar 24 cuotas");
    if (r.primerVencimiento().isBefore(LocalDate.now(clock)))
      throw new BusinessException("El primer vencimiento no puede ser una fecha pasada");
    if (r.primerVencimiento().getDayOfMonth() != r.diaVencimiento())
      throw new BusinessException(
          "El primer vencimiento debe coincidir con el día mensual indicado");
  }

  private void validarMesCompromiso(CompromisoPago c, YearMonth mes) {
    long indice = ChronoUnit.MONTHS.between(c.getMesInicio(), mes);
    if (indice < 0 || indice >= c.getNumeroCuotas())
      throw new BusinessException("El mes no pertenece al cronograma del compromiso");
  }

  private CompromisoPago buscarCompromiso(Integer id) {
    return compromisos
        .findById(id)
        .filter(c -> Boolean.TRUE.equals(c.getActive()))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe el compromiso de pago"));
  }

  private BoletaPension buscarBoleta(Integer id) {
    return boletas
        .findByIdAndActiveTrue(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe la boleta de pensión"));
  }

  private BoletaPension buscarBoletaParaPago(Integer id) {
    return boletas
        .findByIdAndActiveTrueForUpdate(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe la boleta de pensión"));
  }

  private CompromisoPagoResponse respuesta(CompromisoPago c) {
    return new CompromisoPagoResponse(
        c.getId(),
        c.getMatricula().getId(),
        c.getMatricula().getEstudiante().getId(),
        c.getApoderado().getId(),
        c.getConcepto(),
        c.getMontoTotal(),
        c.getFechaEmision(),
        c.getMesInicio(),
        c.getNumeroCuotas(),
        c.getPrimerVencimiento(),
        c.getDiaVencimiento());
  }

  private BoletaPensionResponse respuesta(BoletaPension b) {
    var m = b.getCompromiso().getMatricula();
    var e = m.getEstudiante();
    var p = m.getPeriodo();
    return new BoletaPensionResponse(
        b.getId(),
        b.getCompromiso().getId(),
        m.getId(),
        e.getId(),
        e.getPersona().getNombreCompleto(),
        b.getCompromiso().getApoderado().getId(),
        b.getCompromiso().getApoderado().getPersona().getNombreCompleto(),
        p.getGrado().getNumero(),
        p.getSeccion().getCodigo(),
        b.getMesCobro(),
        b.getMonto(),
        b.getMora(),
        b.calcularMontoTotal(),
        b.getFechaVencimiento(),
        b.getEstadoPago(),
        b.getTipoCargo(),
        b.getMecanismoGeneracion(),
        b.getFechaGeneracion());
  }

  private ComprobantePagoResponse respuesta(ComprobantePago c) {
    return new ComprobantePagoResponse(
        c.getId(),
        c.getBoleta().getId(),
        c.getFechaEmision(),
        c.getMontoPagado(),
        c.getMedioPago(),
        c.getNumeroComprobante());
  }

  private void registrarAuditoria(AccionAuditoria accion, String descripcion) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      auditoria.save(new RegistroAuditoria(null, "SISTEMA", accion, descripcion));
      return;
    }
    var usuario = usuarios.findByUsername(auth.getName()).orElse(null);
    String rol =
        usuario == null
            ? auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("SISTEMA")
                .replace("ROLE_", "")
            : usuario.getRol().name();
    auditoria.save(
        new RegistroAuditoria(usuario == null ? null : usuario.getId(), rol, accion, descripcion));
  }
}
