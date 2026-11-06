package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.dto.*;
import pe.edu.pucp.colegio.common.service.ConsolidacionLogroService;

@RestController
public class ConsolidacionLogroController {

  private final ConsolidacionLogroService consolidacion;

  public ConsolidacionLogroController(ConsolidacionLogroService consolidacion) {
    this.consolidacion = consolidacion;
  }

  @GetMapping("/api/v1/competencias/{codigo}/regla-consolidacion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ReglaConsolidacionResponse obtenerRegla(@PathVariable String codigo) {
    return consolidacion.obtenerRegla(codigo);
  }

  @PutMapping("/api/v1/competencias/{codigo}/regla-consolidacion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ReglaConsolidacionResponse actualizarRegla(
      @PathVariable String codigo,
      @Valid @RequestBody ReglaConsolidacionRequest request,
      Authentication authentication) {
    return consolidacion.actualizarRegla(codigo, request, authentication.getName());
  }

  @PostMapping("/api/v1/evaluaciones-parciales")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<EvaluacionParcialResponse> crearEvaluacion(
      @Valid @RequestBody EvaluacionParcialRequest request, Authentication authentication) {
    return ResponseEntity.status(201)
        .body(consolidacion.crearEvaluacion(request, authentication.getName()));
  }

  @GetMapping("/api/v1/evaluaciones-parciales")
  @PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
  public List<EvaluacionParcialResponse> listarEvaluaciones(
      @RequestParam Integer periodoId,
      @RequestParam String competenciaCodigo,
      Authentication authentication) {
    return consolidacion.listarEvaluaciones(
        periodoId, competenciaCodigo, authentication.getName(), esAdministrador(authentication));
  }

  @PutMapping("/api/v1/evaluaciones-parciales/{evaluacionId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public EvaluacionParcialResponse actualizarEvaluacion(
      @PathVariable Integer evaluacionId,
      @Valid @RequestBody EvaluacionParcialActualizacionRequest request,
      Authentication authentication) {
    return consolidacion.actualizarEvaluacion(evaluacionId, request, authentication.getName());
  }

  @DeleteMapping("/api/v1/evaluaciones-parciales/{evaluacionId}")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<Void> eliminarEvaluacion(
      @PathVariable Integer evaluacionId, Authentication authentication) {
    consolidacion.eliminarEvaluacion(evaluacionId, authentication.getName());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/v1/evaluaciones-parciales/{evaluacionId}/calificaciones")
  @PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
  public List<CalificacionParcialResponse> listarCalificaciones(
      @PathVariable Integer evaluacionId, Authentication authentication) {
    return consolidacion.listarCalificacionesParciales(
        evaluacionId, authentication.getName(), esAdministrador(authentication));
  }

  @PutMapping(
      value = "/api/v1/evaluaciones-parciales/{evaluacionId}/matriculas/{matriculaId}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
  public CalificacionParcialResponse registrarCalificacion(
      @PathVariable Integer evaluacionId,
      @PathVariable Integer matriculaId,
      @Valid @RequestBody CalificacionParcialRequest request,
      Authentication authentication) {
    return consolidacion.registrarCalificacionParcial(
        evaluacionId,
        matriculaId,
        request,
        authentication.getName(),
        esAdministrador(authentication));
  }

  @PutMapping(
      value = "/api/v1/evaluaciones-parciales/{evaluacionId}/matriculas/{matriculaId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
  public CalificacionParcialResponse registrarCalificacionConArchivo(
      @PathVariable Integer evaluacionId,
      @PathVariable Integer matriculaId,
      @Valid @RequestPart("datos") CalificacionParcialRequest request,
      @RequestPart(value = "archivo", required = false) MultipartFile archivo,
      Authentication authentication) {
    return consolidacion.registrarCalificacionParcial(
        evaluacionId,
        matriculaId,
        request,
        archivo,
        authentication.getName(),
        esAdministrador(authentication));
  }

  @GetMapping("/api/v1/evaluaciones-parciales/calificaciones/{calificacionId}/archivo")
  @PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
  public ResponseEntity<Resource> descargarEvidenciaParcial(
      @PathVariable Integer calificacionId, Authentication authentication) {
    Resource recurso =
        consolidacion.descargarEvidenciaParcial(
            calificacionId, authentication.getName(), esAdministrador(authentication));
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"evidencia\"")
        .body(recurso);
  }

  @PostMapping("/api/v1/logros/recalculo")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public RecalculoLogrosResponse recalcular(
      @RequestBody RecalculoLogrosRequest request, Authentication authentication) {
    return consolidacion.recalcular(request, authentication.getName());
  }

  @PostMapping("/api/v1/periodos/{periodoId}/cierre-actas")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public CierreActasResponse cerrarActas(
      @PathVariable Integer periodoId, Authentication authentication) {
    return consolidacion.cerrarActas(periodoId, authentication.getName());
  }

  @GetMapping("/api/v1/periodos/{periodoId}/cierre-actas")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public CierreActasResponse consultarCierreActas(@PathVariable Integer periodoId) {
    return consolidacion.consultarCierreActas(periodoId);
  }

  private boolean esAdministrador(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(autoridad -> autoridad.getAuthority().equals("ROLE_ADMINISTRADOR"));
  }
}
