package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.ComprobantePagoResponse;
import pe.edu.pucp.colegio.common.dto.PagoApoderadoRequest;
import pe.edu.pucp.colegio.common.dto.PagosHijoResponse;
import pe.edu.pucp.colegio.common.service.PagosApoderadoService;

/** Pensiones de los hijos del apoderado autenticado (HU 1.8 y 1.12). */
@RestController
@RequestMapping("/api/v1/apoderados/mis-pagos")
@PreAuthorize("hasRole('APODERADO')")
public class PagosApoderadoController {

  private final PagosApoderadoService pagos;

  public PagosApoderadoController(PagosApoderadoService pagos) {
    this.pagos = pagos;
  }

  @GetMapping
  public List<PagosHijoResponse> listar(Authentication authentication) {
    return pagos.listar(authentication.getName());
  }

  @PostMapping("/pagos")
  public ResponseEntity<List<ComprobantePagoResponse>> pagar(
      @Valid @RequestBody PagoApoderadoRequest request, Authentication authentication) {
    return ResponseEntity.status(201).body(pagos.pagar(authentication.getName(), request));
  }

  @GetMapping(value = "/comprobantes/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> descargarComprobante(
      @PathVariable Integer id, Authentication authentication) {
    PagosApoderadoService.Comprobante comprobante =
        pagos.comprobantePdf(authentication.getName(), id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("comprobante-" + comprobante.numero() + ".pdf")
                .build()
                .toString())
        .body(comprobante.contenido());
  }
}
