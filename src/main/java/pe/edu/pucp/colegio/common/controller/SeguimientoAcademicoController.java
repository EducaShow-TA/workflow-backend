package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.dto.*;
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.service.EvidenciaStorageService;
import pe.edu.pucp.colegio.common.service.SeguimientoAcademicoService;

@RestController
@PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
public class SeguimientoAcademicoController {
  private final SeguimientoAcademicoService service;
  private final EvidenciaStorageService storage;

  public SeguimientoAcademicoController(SeguimientoAcademicoService service, EvidenciaStorageService storage) {
    this.service = service;
    this.storage = storage;
  }

  @PostMapping("/api/v1/tareas")
  public ResponseEntity<TareaResponse> crearTarea(@Valid @RequestBody TareaRequest request) {
    return ResponseEntity.ok(service.crearTarea(request));
  }

  @GetMapping("/api/v1/tareas")
  public List<TareaResponse> listarTareas(@RequestParam Integer periodoId) {
    return service.listarTareas(periodoId);
  }

  @PatchMapping("/api/v1/tareas/{tareaId}/entregas/{matriculaId}")
  public TareaResponse marcarEntrega(@PathVariable Integer tareaId, @PathVariable Integer matriculaId,
      @Valid @RequestBody EstadoEntregaRequest request) {
    return service.marcarEntrega(tareaId, matriculaId, request.cumplida());
  }

  @PostMapping(value = "/api/v1/calificaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CalificacionResponse> registrarCalificacion(
      @Valid @RequestPart("datos") CalificacionRequest request,
      @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
    return ResponseEntity.ok(service.registrarCalificacion(request, archivo));
  }

  @GetMapping("/api/v1/seguimiento/competencias/{codigo}")
  public List<CalificacionResponse> seguimientoCompetencia(@PathVariable String codigo, @RequestParam Integer periodoId) {
    return service.seguimientoCompetencia(periodoId, codigo);
  }

  @GetMapping("/api/v1/evidencias/{calificacionId}/archivo")
  public ResponseEntity<Resource> descargarEvidencia(@PathVariable Integer calificacionId) {
    Calificacion calificacion = service.obtenerCalificacion(calificacionId);
    if (calificacion.getEvidenciaArchivo() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La evidencia es una URL externa");
    }
    Resource recurso = storage.cargar(calificacion.getEvidenciaArchivo());
    MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
    if (calificacion.getEvidenciaTipo() != null) {
      try { tipo = MediaType.parseMediaType(calificacion.getEvidenciaTipo()); }
      catch (IllegalArgumentException ignored) { /* se entrega como binario */ }
    }
    String nombre = calificacion.getEvidenciaNombre() == null ? "evidencia" : calificacion.getEvidenciaNombre().replaceAll("[\\r\\n\\\"]", "_");
    return ResponseEntity.ok().contentType(tipo)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
        .body(recurso);
  }
}
