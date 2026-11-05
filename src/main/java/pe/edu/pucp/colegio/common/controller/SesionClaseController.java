package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteResponse;
import pe.edu.pucp.colegio.common.dto.SesionClaseRequest;
import pe.edu.pucp.colegio.common.dto.SesionClaseResponse;
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
  public List<SesionClaseResponse> listar(
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(required = false) Integer seccionId) {
    return sesiones.listar(fecha, seccionId);
  }

  @GetMapping("/{sesionId}")
  public SesionClaseResponse obtener(@PathVariable Integer sesionId) {
    return sesiones.obtener(sesionId);
  }

  @PostMapping("/{sesionId}/asistencias")
  public ResponseEntity<AsistenciaEstudianteResponse> registrarAsistencia(
      @PathVariable Integer sesionId, @Valid @RequestBody AsistenciaEstudianteRequest request) {
    return ResponseEntity.status(201).body(sesiones.registrarAsistencia(sesionId, request));
  }

  @GetMapping("/{sesionId}/asistencias")
  public List<AsistenciaEstudianteResponse> listarAsistencias(@PathVariable Integer sesionId) {
    return sesiones.listarAsistencias(sesionId);
  }

  @PatchMapping("/{sesionId}/asistencias/{estudianteId}")
  public AsistenciaEstudianteResponse ajustarAsistencia(
      @PathVariable Integer sesionId,
      @PathVariable Integer estudianteId,
      @Valid @RequestBody AsistenciaEstudianteAjusteRequest request) {
    return sesiones.ajustarAsistencia(sesionId, estudianteId, request);
  }
}
