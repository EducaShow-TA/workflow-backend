package pe.edu.pucp.colegio.common.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.config.RelojSistema;
import pe.edu.pucp.colegio.common.dto.EstadoSistemaResponse;
import pe.edu.pucp.colegio.common.dto.FechaSistemaRequest;
import pe.edu.pucp.colegio.common.dto.ParametrosSistemaResponse;
import pe.edu.pucp.colegio.common.model.ParametrosSistema;
import pe.edu.pucp.colegio.common.repository.ParametrosSistemaRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class ParametrosSistemaService {

  private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final ParametrosSistemaRepository parametros;
  private final RelojSistema reloj;
  private final CuentaUsuarioRepository cuentas;
  private final RegistroAuditoriaRepository auditorias;
  private final boolean fechaSimuladaPermitida;
  private final boolean pagoEnLineaPermitido;

  public ParametrosSistemaService(
      ParametrosSistemaRepository parametros,
      RelojSistema reloj,
      CuentaUsuarioRepository cuentas,
      RegistroAuditoriaRepository auditorias,
      @Value("${app.demo.fecha-permitida:false}") boolean fechaSimuladaPermitida,
      @Value("${app.pagos.simulado.enabled:false}") boolean pagoEnLineaPermitido) {
    this.parametros = parametros;
    this.reloj = reloj;
    this.cuentas = cuentas;
    this.auditorias = auditorias;
    this.fechaSimuladaPermitida = fechaSimuladaPermitida;
    this.pagoEnLineaPermitido = pagoEnLineaPermitido;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void aplicarFechaGuardada() {
    Long desfase =
        parametros
            .findById(ParametrosSistema.ID_UNICO)
            .map(ParametrosSistema::getDesfaseSegundos)
            .orElse(null);
    reloj.ajustar(
        fechaSimuladaPermitida && desfase != null ? Duration.ofSeconds(desfase) : Duration.ZERO);
  }

  @Transactional(readOnly = true)
  public ParametrosSistemaResponse obtener() {
    ParametrosSistema actual = actual();
    boolean simulada = fechaSimuladaPermitida && reloj.simulado();
    return new ParametrosSistemaResponse(
        LocalDateTime.now(reloj),
        simulada,
        fechaSimuladaPermitida,
        simulada ? actual.getMotivoFecha() : null,
        simulada ? actual.getFechaSimuladaPor() : null,
        simulada ? actual.getFechaSimuladaEn() : null,
        pagoEnLineaPermitido,
        actual.isPagoEnLineaActivo());
  }

  @Transactional(readOnly = true)
  public EstadoSistemaResponse estado() {
    return new EstadoSistemaResponse(
        LocalDateTime.now(reloj),
        fechaSimuladaPermitida && reloj.simulado(),
        pagoEnLineaDisponible());
  }

  @Transactional(readOnly = true)
  public boolean pagoEnLineaDisponible() {
    return pagoEnLineaPermitido && actual().isPagoEnLineaActivo();
  }

  @Transactional
  public ParametrosSistemaResponse simularFecha(FechaSistemaRequest request, String username) {
    if (!fechaSimuladaPermitida) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La fecha simulada no está habilitada en este servidor");
    }
    if (request.fecha().getYear() < 2020 || request.fecha().getYear() > 2100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Elige una fecha entre 2020 y 2100");
    }
    LocalDateTime anterior = LocalDateTime.now(reloj);
    LocalDateTime ahoraReal = LocalDateTime.now(reloj.real()).truncatedTo(ChronoUnit.SECONDS);
    long desfase = Duration.between(ahoraReal, request.fecha()).toSeconds();
    String motivo = request.motivo().trim();
    ParametrosSistema actual = actual();
    actual.simularFecha(desfase, motivo, username, ahoraReal);
    parametros.save(actual);
    reloj.ajustar(Duration.ofSeconds(desfase));
    auditar(
        username,
        AccionAuditoria.CAMBIAR_FECHA_SISTEMA,
        "Fecha del sistema simulada para la demostración: "
            + request.fecha().format(FORMATO)
            + " (antes "
            + anterior.format(FORMATO)
            + "). Motivo: "
            + motivo);
    return obtener();
  }

  @Transactional
  public ParametrosSistemaResponse volverAFechaReal(String username) {
    if (!reloj.simulado()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El sistema ya usa la fecha real");
    }
    LocalDateTime simulada = LocalDateTime.now(reloj);
    ParametrosSistema actual = actual();
    actual.volverAFechaReal();
    parametros.save(actual);
    reloj.ajustar(Duration.ZERO);
    auditar(
        username,
        AccionAuditoria.CAMBIAR_FECHA_SISTEMA,
        "El sistema volvió a la fecha real (la fecha simulada era "
            + simulada.format(FORMATO)
            + ")");
    return obtener();
  }

  @Transactional
  public ParametrosSistemaResponse configurarPagoEnLinea(boolean activo, String username) {
    if (!pagoEnLineaPermitido) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El servidor no permite el pago en línea simulado");
    }
    ParametrosSistema actual = actual();
    if (actual.isPagoEnLineaActivo() == activo) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay cambios que guardar");
    }
    actual.setPagoEnLineaActivo(activo);
    parametros.save(actual);
    auditar(
        username,
        AccionAuditoria.CONFIGURAR_PAGO_EN_LINEA,
        activo
            ? "Pago en línea disponible para los apoderados"
            : "Pago en línea desactivado para los apoderados");
    return obtener();
  }

  private ParametrosSistema actual() {
    return parametros
        .findById(ParametrosSistema.ID_UNICO)
        .orElseGet(() -> new ParametrosSistema(ParametrosSistema.ID_UNICO));
  }

  private void auditar(String username, AccionAuditoria accion, String descripcion) {
    CuentaUsuario cuenta = cuentas.findByUsername(username).orElse(null);
    auditorias.save(
        new RegistroAuditoria(
            cuenta == null ? null : cuenta.getId(),
            cuenta == null ? null : cuenta.getRol().name(),
            accion,
            descripcion));
  }
}
