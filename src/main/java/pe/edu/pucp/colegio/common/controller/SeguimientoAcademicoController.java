package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.*;
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.service.EvidenciaStorageService;
import pe.edu.pucp.colegio.common.service.SeguimientoAcademicoService;
import pe.edu.pucp.colegio.common.service.MisClasesService;

@RestController
@PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
public class SeguimientoAcademicoController {
  private final SeguimientoAcademicoService service;
  private final EvidenciaStorageService storage;
  private final MisClasesService misClases;

  public SeguimientoAcademicoController(
      SeguimientoAcademicoService service,
      EvidenciaStorageService storage,
      MisClasesService misClases) {
    this.service = service;
    this.storage = storage;
    this.misClases = misClases;
  }

  @PostMapping("/api/v1/tareas")
  public ResponseEntity<TareaResponse> crearTarea(
      @Valid @RequestBody TareaRequest request, Authentication authentication) {
    validarAccesoCompetenciaEnPeriodo(request.periodoId(), request.competenciaCodigo(), authentication);
    return ResponseEntity.ok(service.crearTarea(request));
  }

  @GetMapping("/api/v1/tareas")
  public List<TareaResponse> listarTareas(
      @RequestParam Integer periodoId, Authentication authentication) {
    List<TareaResponse> tareas = service.listarTareas(periodoId);
    if (esAdministrador(authentication)) return tareas;
    Set<String> competencias = competenciasDelDocente(authentication, periodoId);
    return tareas.stream()
        .filter(tarea -> competencias.contains(tarea.competenciaCodigo()))
        .toList();
  }

  @PatchMapping("/api/v1/tareas/{tareaId}/entregas/{matriculaId}")
  public TareaResponse marcarEntrega(@PathVariable Integer tareaId, @PathVariable Integer matriculaId,
      @Valid @RequestBody EstadoEntregaRequest request, Authentication authentication) {
    if (!esAdministrador(authentication)) {
      boolean tareaAsignada = misClases.listar(authentication.getName()).stream()
          .flatMap(clase -> service.listarTareas(clase.periodoId()).stream())
          .anyMatch(tarea -> tarea.id().equals(tareaId)
              && competenciasDelDocente(authentication, tarea.periodoId())
                  .contains(tarea.competenciaCodigo()));
      if (!tareaAsignada) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La tarea no pertenece a tus clases");
      }
    }
    return service.marcarEntrega(tareaId, matriculaId, request.cumplida());
  }

  private void validarAccesoCompetenciaEnPeriodo(
      Integer periodoId, String competenciaCodigo, Authentication authentication) {
    if (esAdministrador(authentication)
        || competenciasDelDocente(authentication, periodoId).contains(competenciaCodigo)) return;
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes gestionar tareas de tus clases");
  }

  private Set<String> competenciasDelDocente(Authentication authentication, Integer periodoId) {
    return misClases.listar(authentication.getName()).stream()
        .filter(clase -> clase.periodoId().equals(periodoId))
        .flatMap(clase -> clase.competencias().stream())
        .map(CompetenciaClaseResponse::codigo)
        .collect(Collectors.toSet());
  }

  private static boolean esAdministrador(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMINISTRADOR".equals(authority.getAuthority()));
  }

  @PostMapping(value = "/api/v1/calificaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CalificacionResponse> registrarCalificacion(
      @Valid @RequestPart("datos") CalificacionRequest request,
      @RequestPart(value = "archivo", required = false) MultipartFile archivo,
      Authentication authentication) {
    validarAccesoCompetenciaEnPeriodo(request.periodoId(), request.competenciaCodigo(), authentication);
    String rol =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(autoridad -> autoridad.startsWith("ROLE_"))
            .map(autoridad -> autoridad.substring("ROLE_".length()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("La cuenta autenticada no tiene un rol asignado"));
    return ResponseEntity.ok(
        service.registrarCalificacion(request, archivo, authentication.getName(), rol));
  }

  @GetMapping("/api/v1/seguimiento/competencias/{codigo}")
  public List<CalificacionResponse> seguimientoCompetencia(
      @PathVariable String codigo,
      @RequestParam Integer periodoId,
      Authentication authentication) {
    validarAccesoCompetenciaEnPeriodo(periodoId, codigo, authentication);
    return service.seguimientoCompetencia(periodoId, codigo);
  }

  @GetMapping("/api/v1/evidencias/{calificacionId}/archivo")
  public ResponseEntity<Resource> descargarEvidencia(
      @PathVariable Integer calificacionId, Authentication authentication) {
    Calificacion calificacion = service.obtenerCalificacion(calificacionId);
    validarAccesoCompetenciaEnPeriodo(
        calificacion.getPeriodo().getId(),
        calificacion.getCompetencia().getCodigo(),
        authentication);
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
