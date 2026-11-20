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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.ComentarioLibretaRequest;
import pe.edu.pucp.colegio.common.dto.ComentarioLibretaResponse;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse;
import pe.edu.pucp.colegio.common.service.ComentarioLibretaService;
import pe.edu.pucp.colegio.common.service.LibretaNotasService;
import pe.edu.pucp.colegio.common.service.LibretaPdfService;

/** Expone la libreta, el PDF y el comentario cualitativo del período. */
@RestController
@RequestMapping("/api/v1/estudiantes/{estudianteId}/libreta")
public class LibretaNotasController {

  private final LibretaNotasService libretas;
  private final LibretaPdfService pdf;
  private final ComentarioLibretaService comentarios;

  public LibretaNotasController(
      LibretaNotasService libretas, LibretaPdfService pdf, ComentarioLibretaService comentarios) {
    this.libretas = libretas;
    this.pdf = pdf;
    this.comentarios = comentarios;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE', 'APODERADO', 'ALUMNO')")
  public LibretaNotasResponse generar(
      @PathVariable Integer estudianteId,
      @RequestParam Integer periodoId,
      @RequestParam Integer bimestre,
      Authentication authentication) {
    return libretas.generar(estudianteId, periodoId, bimestre, authentication.getName());
  }

  @GetMapping("/comentario-periodo")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE', 'APODERADO', 'ALUMNO')")
  public ComentarioLibretaResponse comentarioPeriodo(
      @PathVariable Integer estudianteId,
      @RequestParam Integer periodoId,
      Authentication authentication) {
    return comentarios.obtener(estudianteId, periodoId, authentication.getName());
  }

  @PutMapping("/comentario-periodo")
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
  public ComentarioLibretaResponse guardarComentarioPeriodo(
      @PathVariable Integer estudianteId,
      @RequestParam Integer periodoId,
      @jakarta.validation.Valid @RequestBody ComentarioLibretaRequest request,
      Authentication authentication) {
    return comentarios.guardar(estudianteId, periodoId, request, authentication.getName());
  }

  @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE', 'APODERADO')")
  public ResponseEntity<byte[]> descargarPdf(
      @PathVariable Integer estudianteId,
      @RequestParam Integer periodoId,
      @RequestParam Integer bimestre,
      Authentication authentication) {
    LibretaNotasResponse libreta =
        libretas.generar(estudianteId, periodoId, bimestre, authentication.getName());
    ComentarioLibretaResponse comentario =
        comentarios.obtener(estudianteId, periodoId, authentication.getName());
    libreta =
        new LibretaNotasResponse(
            libreta.estudianteId(),
            libreta.estudiante(),
            libreta.periodoSolicitadoId(),
            libreta.anhioEscolar(),
            libreta.bimestre(),
            libreta.evaluacionesSinBimestre(),
            libreta.periodos(),
            comentario.comentario(),
            comentario.autorNombre());
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
            + "-bimestre-"
            + bimestre
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
