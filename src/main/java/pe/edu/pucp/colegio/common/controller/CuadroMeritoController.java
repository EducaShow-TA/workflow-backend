package pe.edu.pucp.colegio.common.controller;

import org.springframework.http.CacheControl;
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
  public CuadroMeritoResponse consultar(
      @RequestParam(required = false) Integer periodoId,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer anio,
      @RequestParam(required = false) Integer seccionId) {
    return generar(periodoId, gradoId, anio, seccionId);
  }

  @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> descargarPdf(
      @RequestParam(required = false) Integer periodoId,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer anio,
      @RequestParam(required = false) Integer seccionId) {
    CuadroMeritoResponse cuadro = generar(periodoId, gradoId, anio, seccionId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(nombreArchivo(cuadro, "pdf"))
                .build()
                .toString())
        .body(documentos.generarPdf(cuadro));
  }

  @GetMapping(
      value = "/excel",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> descargarExcel(
      @RequestParam(required = false) Integer periodoId,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer anio,
      @RequestParam(required = false) Integer seccionId) {
    CuadroMeritoResponse cuadro = generar(periodoId, gradoId, anio, seccionId);
    return ResponseEntity.ok()
        .contentType(EXCEL)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(nombreArchivo(cuadro, "xlsx"))
                .build()
                .toString())
        .body(documentos.generarExcel(cuadro));
  }

  private CuadroMeritoResponse generar(
      Integer periodoId, Integer gradoId, Integer anio, Integer seccionId) {
    if (periodoId != null) {
      if (gradoId != null || anio != null || seccionId != null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Use periodoId o el filtro gradoId/anio/seccionId");
      }
      return cuadros.generar(periodoId);
    }
    return cuadros.generar(gradoId, anio, seccionId);
  }

  private String nombreArchivo(CuadroMeritoResponse cuadro, String extension) {
    String prefijo =
        cuadro.periodoId() == null
            ? "cuadro-merito-grado-" + cuadro.gradoNumero() + "-" + cuadro.anhioEscolar()
            : "cuadro-merito-periodo-" + cuadro.periodoId();
    return prefijo + "." + extension;
  }
}
