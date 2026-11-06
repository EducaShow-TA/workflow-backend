package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteResponse;
import pe.edu.pucp.colegio.common.dto.ListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.RegistroAsistenciaSesionRequest;
import pe.edu.pucp.colegio.common.dto.SesionClaseRequest;
import pe.edu.pucp.colegio.common.dto.SesionClaseResponse;
import pe.edu.pucp.colegio.common.dto.SesionClaseResumenResponse;
import pe.edu.pucp.colegio.common.service.SesionClaseService;

/** Endpoints para sesiones de clase y asistencia de alumnos por sesión. */
@RestController
@RequestMapping("/api/v1/sesiones-clase")
@PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
public class SesionClaseController {

  private final SesionClaseService sesiones;

  public SesionClaseController(SesionClaseService sesiones) {
    this.sesiones = sesiones;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ResponseEntity<SesionClaseResponse> crear(@Valid @RequestBody SesionClaseRequest request) {
    return ResponseEntity.status(201).body(sesiones.crear(request));
  }

  @GetMapping
  public List<SesionClaseResumenResponse> listar(
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(required = false) Integer seccionId) {
    return sesiones.listar(fecha, seccionId);
  }

  @GetMapping("/mias")
  @PreAuthorize("hasRole('DOCENTE')")
  public List<SesionClaseResumenResponse> listarMias(
      @RequestParam(required = false) LocalDate fecha, Authentication authentication) {
    return sesiones.listarDelDocente(authentication.getName(), fecha);
  }

  @GetMapping("/{sesionId}/lista")
  public ListaAsistenciaResponse obtenerLista(
      @PathVariable Integer sesionId, Authentication authentication) {
    return sesiones.obtenerLista(sesionId, acceso(authentication));
  }

  @PutMapping("/{sesionId}/asistencias")
  public ListaAsistenciaResponse guardarAsistencias(
      @PathVariable Integer sesionId,
      @Valid @RequestBody RegistroAsistenciaSesionRequest request,
      Authentication authentication) {
    return sesiones.guardarAsistencias(sesionId, request, acceso(authentication));
  }

  @GetMapping("/{sesionId}")
  public SesionClaseResponse obtener(@PathVariable Integer sesionId) {
    return sesiones.obtener(sesionId);
  }

  @PostMapping("/{sesionId}/asistencias")
  public ResponseEntity<AsistenciaEstudianteResponse> registrarAsistencia(
      @PathVariable Integer sesionId,
      @Valid @RequestBody AsistenciaEstudianteRequest request,
      Authentication authentication) {
    return ResponseEntity.status(201)
        .body(sesiones.registrarAsistencia(sesionId, request, acceso(authentication)));
  }

  @GetMapping("/{sesionId}/asistencias")
  public List<AsistenciaEstudianteResponse> listarAsistencias(@PathVariable Integer sesionId) {
    return sesiones.listarAsistencias(sesionId);
  }

  @PatchMapping("/{sesionId}/asistencias/{estudianteId}")
  public AsistenciaEstudianteResponse ajustarAsistencia(
      @PathVariable Integer sesionId,
      @PathVariable Integer estudianteId,
      @Valid @RequestBody AsistenciaEstudianteAjusteRequest request,
      Authentication authentication) {
    return sesiones.ajustarAsistencia(sesionId, estudianteId, request, acceso(authentication));
  }

  private static SesionClaseService.Acceso acceso(Authentication authentication) {
    boolean administrador =
        authentication.getAuthorities().stream()
            .anyMatch(autoridad -> "ROLE_ADMINISTRADOR".equals(autoridad.getAuthority()));
    return new SesionClaseService.Acceso(authentication.getName(), administrador);
  }
}
