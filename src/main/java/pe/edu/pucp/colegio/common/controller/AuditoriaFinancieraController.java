package pe.edu.pucp.colegio.common.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoriaFinanciera;
import pe.edu.pucp.colegio.common.dto.AuditoriaFinancieraResponse;
import pe.edu.pucp.colegio.common.dto.AuditoriaFinancieraResumenResponse;
import pe.edu.pucp.colegio.common.dto.FiltroAuditoriaFinanciera;
import pe.edu.pucp.colegio.common.model.MedioPago;
import pe.edu.pucp.colegio.common.service.AuditoriaFinancieraService;

/** Auditoría de transacciones y pagos (HU 5.4), solo para el administrador. */
@RestController
@RequestMapping("/api/v1/auditoria-finanzas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaFinancieraController {

  private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private final AuditoriaFinancieraService auditoria;

  public AuditoriaFinancieraController(AuditoriaFinancieraService auditoria) {
    this.auditoria = auditoria;
  }

  @GetMapping
  public PagedModel<AuditoriaFinancieraResponse> listar(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam(required = false) AccionAuditoriaFinanciera accion,
      @RequestParam(required = false) MedioPago medioPago,
      @RequestParam(required = false) String estudiante,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId,
      @RequestParam(required = false) String comprobante,
      @RequestParam(required = false) Integer boletaId,
      @PageableDefault(
              size = 20,
              sort = {"fechaHora", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    if (pageable.getPageSize() > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El tamaño de página no puede ser mayor a 100");
    }
    return new PagedModel<>(
        auditoria.listar(
            new FiltroAuditoriaFinanciera(
                desde,
                hasta,
                accion,
                medioPago,
                estudiante,
                gradoId,
                seccionId,
                comprobante,
                boletaId),
            pageable));
  }

  @GetMapping("/resumen")
  public AuditoriaFinancieraResumenResponse resumir(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam(required = false) AccionAuditoriaFinanciera accion,
      @RequestParam(required = false) MedioPago medioPago,
      @RequestParam(required = false) String estudiante,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId,
      @RequestParam(required = false) String comprobante,
      @RequestParam(required = false) Integer boletaId) {
    return auditoria.resumir(
        new FiltroAuditoriaFinanciera(
            desde,
            hasta,
            accion,
            medioPago,
            estudiante,
            gradoId,
            seccionId,
            comprobante,
            boletaId));
  }

  @GetMapping("/csv")
  public ResponseEntity<byte[]> exportarCsv(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam(required = false) AccionAuditoriaFinanciera accion,
      @RequestParam(required = false) MedioPago medioPago,
      @RequestParam(required = false) String estudiante,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId,
      @RequestParam(required = false) String comprobante,
      @RequestParam(required = false) Integer boletaId) {
    String csv =
        auditoria.exportarCsv(
            new FiltroAuditoriaFinanciera(
                desde,
                hasta,
                accion,
                medioPago,
                estudiante,
                gradoId,
                seccionId,
                comprobante,
                boletaId));
    String nombre =
        "auditoria-finanzas"
            + (desde == null ? "" : "_" + desde)
            + (hasta == null ? "" : "_" + hasta)
            + ".csv";
    return ResponseEntity.ok()
        .contentType(TEXT_CSV)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(nombre).build().toString())
        .body(csv.getBytes(StandardCharsets.UTF_8));
  }
}
