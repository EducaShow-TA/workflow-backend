package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.ComprobantePagoResponse;
import pe.edu.pucp.colegio.common.dto.PagoApoderadoRequest;
import pe.edu.pucp.colegio.common.dto.PagosHijoResponse;
import pe.edu.pucp.colegio.common.dto.PagosHijoResponse.BoletaApoderado;
import pe.edu.pucp.colegio.common.dto.PagosHijoResponse.ComprobanteApoderado;
import pe.edu.pucp.colegio.common.dto.PagosHijoResponse.CompromisoApoderado;
import pe.edu.pucp.colegio.common.dto.PagosHijoResponse.Situacion;
import pe.edu.pucp.colegio.common.dto.RegistrarPagoRequest;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.ComprobantePago;
import pe.edu.pucp.colegio.common.model.CompromisoPago;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.MedioPago;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.BoletaPensionRepository;
import pe.edu.pucp.colegio.common.repository.ComprobantePagoRepository;
import pe.edu.pucp.colegio.common.repository.CompromisoPagoRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Pensiones de los hijos del apoderado autenticado: consulta, pago simulado y comprobantes. */
@Service
public class PagosApoderadoService {

  private final CuentaUsuarioRepository cuentas;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final CompromisoPagoRepository compromisos;
  private final BoletaPensionRepository boletas;
  private final ComprobantePagoRepository comprobantes;
  private final FinanzasService finanzas;
  private final ComprobantePdfService pdf;
  private final boolean pagoSimuladoEnabled;
  private final ParametrosSistemaService parametros;

  public PagosApoderadoService(
      CuentaUsuarioRepository cuentas,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      CompromisoPagoRepository compromisos,
      BoletaPensionRepository boletas,
      ComprobantePagoRepository comprobantes,
      FinanzasService finanzas,
      ComprobantePdfService pdf,
      ParametrosSistemaService parametros,
      @Value("${app.pagos.simulado.enabled:true}") boolean pagoSimuladoEnabled) {
    this.cuentas = cuentas;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.compromisos = compromisos;
    this.boletas = boletas;
    this.comprobantes = comprobantes;
    this.finanzas = finanzas;
    this.pdf = pdf;
    this.pagoSimuladoEnabled = pagoSimuladoEnabled;
    this.parametros = parametros;
  }

  @Transactional(readOnly = true)
  public List<PagosHijoResponse> listar(String username) {
    Apoderado apoderado = apoderadoActivo(username);
    return vinculos.listarEstudiantesActivosDeApoderado(apoderado.getId()).stream()
        .map(vinculo -> vinculo.getEstudiante())
        .map(
            estudiante ->
                new PagosHijoResponse(
                    estudiante.getId(),
                    estudiante.getPersona().getNombreCompleto(),
                    compromisos.listarVigentesDeEstudiante(estudiante.getId()).stream()
                        .map(this::resumir)
                        .toList()))
        .sorted(Comparator.comparing(PagosHijoResponse::nombreCompleto))
        .toList();
  }

  /** Paga todas las boletas o ninguna: si una falla, la transacción completa se revierte. */
  @Transactional
  public List<ComprobantePagoResponse> pagar(String username, PagoApoderadoRequest request) {
    if (!pagoSimuladoEnabled) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "El pago simulado está deshabilitado");
    }
    if (!parametros.pagoEnLineaDisponible()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "El pago en línea no está disponible en este momento. Puedes pagar en la tesorería del colegio");
    }
    Apoderado apoderado = apoderadoActivo(username);
    if (new HashSet<>(request.boletaIds()).size() != request.boletaIds().size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No se puede incluir la misma boleta dos veces");
    }
    List<BoletaPension> seleccionadas =
        request.boletaIds().stream()
            .map(
                id ->
                    boletas
                        .findByIdAndActiveTrue(id)
                        .orElseThrow(
                            () ->
                                new ResponseStatusException(
                                    HttpStatus.NOT_FOUND, "No existe la boleta de pensión")))
            .sorted(Comparator.comparing(BoletaPension::getMesCobro))
            .toList();
    for (BoletaPension boleta : seleccionadas) {
      exigirHijoVinculado(apoderado, boleta);
      if (boleta.getCompromiso().getMatricula().getEstado() == EstadoMatricula.RETIRADA) {
        throw new ConflictException("La matrícula de la boleta fue retirada");
      }
      if (boleta.getEstadoPago() == EstadoBoleta.PAGADA) {
        throw new ConflictException(
            "La boleta de " + nombrarMes(boleta.getMesCobro()) + " ya tiene un pago registrado");
      }
    }
    return seleccionadas.stream()
        .map(
            boleta ->
                finanzas.registrarPago(
                    boleta.getId(),
                    new RegistrarPagoRequest(
                        boleta.calcularMontoTotal(), MedioPago.VIRTUAL, referenciaSimulada())))
        .toList();
  }

  @Transactional(readOnly = true)
  public Comprobante comprobantePdf(String username, Integer comprobanteId) {
    Apoderado apoderado = apoderadoActivo(username);
    ComprobantePago comprobante =
        comprobantes
            .findById(comprobanteId)
            .filter(c -> Boolean.TRUE.equals(c.getActive()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el comprobante"));
    exigirHijoVinculado(apoderado, comprobante.getBoleta());
    return new Comprobante(comprobante.getNumeroComprobante(), pdf.generar(comprobante));
  }

  public record Comprobante(String numero, byte[] contenido) {}

  private CompromisoApoderado resumir(CompromisoPago compromiso) {
    List<BoletaPension> emitidas =
        boletas.findAllByCompromisoIdAndActiveTrueOrderByMesCobroAsc(compromiso.getId());
    List<BoletaApoderado> detalle = emitidas.stream().map(this::boleta).toList();
    long pagadas = contar(emitidas, EstadoBoleta.PAGADA);
    long enMora = contar(emitidas, EstadoBoleta.EN_MORA);
    long pendientes = contar(emitidas, EstadoBoleta.PENDIENTE);
    BigDecimal pagado =
        detalle.stream()
            .filter(b -> b.comprobante() != null)
            .map(b -> b.comprobante().montoPagado())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2);
    BigDecimal porPagar =
        emitidas.stream()
            .filter(b -> b.getEstadoPago() != EstadoBoleta.PAGADA)
            .map(BoletaPension::calcularMontoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2);
    int porEmitir = Math.max(0, compromiso.getNumeroCuotas() - emitidas.size());
    var periodo = compromiso.getMatricula().getPeriodo();
    return new CompromisoApoderado(
        compromiso.getId(),
        compromiso.getConcepto(),
        periodo.getFechaInicio().getYear(),
        periodo.getGrado().getNumero(),
        periodo.getSeccion().getCodigo(),
        compromiso.getApoderado().getPersona().getNombreCompleto(),
        compromiso.getMontoTotal(),
        pagado,
        porPagar,
        compromiso.getNumeroCuotas(),
        pagadas,
        enMora,
        pendientes,
        porEmitir,
        porEmitir == 0 ? null : compromiso.getMesInicio().plusMonths(emitidas.size()).atDay(1),
        enMora > 0
            ? Situacion.CON_MORA
            : pendientes > 0 ? Situacion.CON_PENDIENTES : Situacion.AL_DIA,
        detalle);
  }

  private BoletaApoderado boleta(BoletaPension boleta) {
    ComprobanteApoderado comprobante =
        comprobantes.findAllByBoletaIdAndActiveTrueOrderByFechaEmisionDesc(boleta.getId()).stream()
            .findFirst()
            .map(
                c ->
                    new ComprobanteApoderado(
                        c.getId(),
                        c.getNumeroComprobante(),
                        c.getFechaEmision(),
                        c.getMedioPago(),
                        c.getMontoPagado()))
            .orElse(null);
    return new BoletaApoderado(
        boleta.getId(),
        boleta.getMesCobro(),
        boleta.getFechaVencimiento(),
        boleta.getMonto(),
        boleta.getMora(),
        boleta.calcularMontoTotal(),
        boleta.getEstadoPago(),
        comprobante);
  }

  private long contar(List<BoletaPension> lista, EstadoBoleta estado) {
    return lista.stream().filter(b -> b.getEstadoPago() == estado).count();
  }

  private void exigirHijoVinculado(Apoderado apoderado, BoletaPension boleta) {
    Integer estudianteId = boleta.getCompromiso().getMatricula().getEstudiante().getId();
    if (!vinculos.existsByEstudianteIdAndApoderadoIdAndActiveTrue(
        estudianteId, apoderado.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "La boleta no pertenece a un estudiante vinculado a tu cuenta");
    }
  }

  private Apoderado apoderadoActivo(String username) {
    var cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .filter(usuario -> usuario.getRol() == Rol.APODERADO && usuario.getPersona() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "La cuenta no corresponde a un apoderado activo"));
    return apoderados
        .findByPersonaIdAndActiveTrue(cuenta.getPersona().getId())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No existe un registro de apoderado activo"));
  }

  private String referenciaSimulada() {
    return "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
  }

  private String nombrarMes(YearMonth mes) {
    return mes.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"))
        + " "
        + mes.getYear();
  }
}
