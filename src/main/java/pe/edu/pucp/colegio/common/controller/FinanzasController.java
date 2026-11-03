package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.BoletaPensionReporteResponse;
import pe.edu.pucp.colegio.common.dto.BoletaPensionResponse;
import pe.edu.pucp.colegio.common.dto.ComprobantePagoResponse;
import pe.edu.pucp.colegio.common.dto.CompromisoPagoResponse;
import pe.edu.pucp.colegio.common.dto.GenerarBoletaManualRequest;
import pe.edu.pucp.colegio.common.dto.RegistrarPagoRequest;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.service.FinanzasService;

/** Operaciones administrativas de compromisos, pensiones, pagos y cobranza. */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class FinanzasController {

  private final FinanzasService finanzas;

  public FinanzasController(FinanzasService finanzas) {
    this.finanzas = finanzas;
  }

  @GetMapping("/compromisos-pago")
  public List<CompromisoPagoResponse> listarCompromisos(
      @RequestParam(required = false) Integer estudianteId) {
    return finanzas.listarCompromisos(estudianteId);
  }

  @GetMapping("/compromisos-pago/{id}")
  public CompromisoPagoResponse obtenerCompromiso(@PathVariable Integer id) {
    return finanzas.obtenerCompromiso(id);
  }

  @GetMapping("/boletas-pension")
  public List<BoletaPensionResponse> listarBoletas(
      @RequestParam(required = false) Integer estudianteId,
      @RequestParam(required = false) Integer periodoId,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
      @RequestParam(required = false) EstadoBoleta estado) {
    return finanzas.listarBoletas(estudianteId, periodoId, mes, estado);
  }

  @GetMapping("/boletas-pension/{id}")
  public BoletaPensionResponse obtenerBoleta(@PathVariable Integer id) {
    return finanzas.obtenerBoleta(id);
  }

  @PostMapping("/boletas-pension/manual")
  public ResponseEntity<BoletaPensionResponse> generarBoletaManual(
      @Valid @RequestBody GenerarBoletaManualRequest request) {
    return ResponseEntity.status(201).body(finanzas.generarManual(request.matriculaId(), request.mesCobro()));
  }

  @PostMapping("/boletas-pension/{id}/pagos")
  public ResponseEntity<ComprobantePagoResponse> registrarPago(
      @PathVariable Integer id, @Valid @RequestBody RegistrarPagoRequest request) {
    return ResponseEntity.status(201).body(finanzas.registrarPago(id, request));
  }

  @GetMapping("/boletas-pension/{id}/comprobantes")
  public List<ComprobantePagoResponse> comprobantes(@PathVariable Integer id) {
    return finanzas.comprobantes(id);
  }

  @GetMapping("/reportes/boletas-pension")
  public BoletaPensionReporteResponse reporteBoletas(
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId,
      @RequestParam(required = false) EstadoBoleta estado,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimientoDesde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimientoHasta) {
    return finanzas.reporte(gradoId, seccionId, estado, vencimientoDesde, vencimientoHasta);
  }
}
