package pe.edu.pucp.colegio.common.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.ReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.SeccionReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.service.ReporteAsistenciaCsv;
import pe.edu.pucp.colegio.common.service.ReporteAsistenciaService;

/**
 * Expone el reporte de inasistencias y tardanzas de alumnos (US-2.6) en JSON y CSV, y el catálogo
 * de secciones que el usuario puede filtrar. El control de acceso por rol vive en el servicio.
 */
@RestController
@RequestMapping("/api/v1/reportes/asistencia-estudiantes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE')")
public class ReporteAsistenciaController {

  private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private final ReporteAsistenciaService reportes;
  private final ReporteAsistenciaCsv csv;

  public ReporteAsistenciaController(ReporteAsistenciaService reportes, ReporteAsistenciaCsv csv) {
    this.reportes = reportes;
    this.csv = csv;
  }

  @GetMapping
  public ReporteAsistenciaResponse consultar(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId,
      Authentication authentication) {
    return reportes.generar(desde, hasta, gradoId, seccionId, authentication.getName());
  }

  @GetMapping("/csv")
  public ResponseEntity<byte[]> exportarCsv(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId,
      Authentication authentication) {
    ReporteAsistenciaResponse reporte =
        reportes.generar(desde, hasta, gradoId, seccionId, authentication.getName());
    String nombre = "reporte-asistencia_" + desde + "_" + hasta + ".csv";
    return ResponseEntity.ok()
        .contentType(TEXT_CSV)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(nombre).build().toString())
        .body(csv.exportar(reporte));
  }

  @GetMapping("/secciones")
  public List<SeccionReporteAsistenciaResponse> listarSecciones(Authentication authentication) {
    return reportes.listarSecciones(authentication.getName());
  }
}
