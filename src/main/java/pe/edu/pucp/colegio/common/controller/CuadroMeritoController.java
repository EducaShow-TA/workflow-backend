package pe.edu.pucp.colegio.common.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.CuadroMeritoResponse;
import pe.edu.pucp.colegio.common.service.CuadroMeritoDocumentoService;
import pe.edu.pucp.colegio.common.service.CuadroMeritoService;

@RestController
@RequestMapping("/api/v1/reportes/cuadro-merito")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CuadroMeritoController {

  private static final MediaType EXCEL =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final CuadroMeritoService cuadros;
  private final CuadroMeritoDocumentoService documentos;

  public CuadroMeritoController(
      CuadroMeritoService cuadros, CuadroMeritoDocumentoService documentos) {
    this.cuadros = cuadros;
    this.documentos = documentos;
  }

  @GetMapping
  public CuadroMeritoResponse consultar(@RequestParam Integer periodoId) {
    return cuadros.generar(periodoId);
  }

  @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> descargarPdf(@RequestParam Integer periodoId) {
    CuadroMeritoResponse cuadro = cuadros.generar(periodoId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("cuadro-merito-periodo-" + periodoId + ".pdf")
                .build()
                .toString())
        .body(documentos.generarPdf(cuadro));
  }

  @GetMapping(
      value = "/excel",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> descargarExcel(@RequestParam Integer periodoId) {
    CuadroMeritoResponse cuadro = cuadros.generar(periodoId);
    return ResponseEntity.ok()
        .contentType(EXCEL)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("cuadro-merito-periodo-" + periodoId + ".xlsx")
                .build()
                .toString())
        .body(documentos.generarExcel(cuadro));
  }
}
