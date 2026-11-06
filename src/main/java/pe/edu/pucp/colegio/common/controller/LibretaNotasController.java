package pe.edu.pucp.colegio.common.controller;

import java.util.Objects;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse;
import pe.edu.pucp.colegio.common.service.LibretaNotasService;
import pe.edu.pucp.colegio.common.service.LibretaPdfService;

@RestController
@RequestMapping("/api/v1/estudiantes/{estudianteId}/libreta")
public class LibretaNotasController {

  private final LibretaNotasService libretas;
  private final LibretaPdfService pdf;

  public LibretaNotasController(LibretaNotasService libretas, LibretaPdfService pdf) {
    this.libretas = libretas;
    this.pdf = pdf;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE', 'APODERADO', 'ALUMNO')")
  public LibretaNotasResponse generar(
      @PathVariable Integer estudianteId,
      @RequestParam Integer periodoId,
      Authentication authentication) {
    return libretas.generar(estudianteId, periodoId, authentication.getName());
  }

  @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE', 'APODERADO')")
  public ResponseEntity<byte[]> descargarPdf(
      @PathVariable Integer estudianteId,
      @RequestParam Integer periodoId,
      Authentication authentication) {
    LibretaNotasResponse libreta =
        libretas.generar(estudianteId, periodoId, authentication.getName());
    boolean preliminar =
        libreta.periodos().stream()
            .filter(periodo -> Objects.equals(periodo.periodoId(), periodoId))
            .findFirst()
            .map(periodo -> !periodo.actasCerradas())
            .orElseThrow(
                () -> new IllegalStateException("La libreta no contiene el período solicitado"));
    String tipoDocumento = preliminar ? "borrador" : "oficial";
    String nombreArchivo =
        "libreta-estudiante-"
            + estudianteId
            + "-periodo-"
            + periodoId
            + "-"
            + tipoDocumento
            + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(nombreArchivo).build().toString())
        .header("X-Documento-Estado", preliminar ? "BORRADOR" : "OFICIAL")
        .body(pdf.generar(libreta, preliminar));
  }
}
