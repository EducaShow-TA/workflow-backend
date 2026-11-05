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
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteCreateRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteResponse;
import pe.edu.pucp.colegio.common.service.AsistenciaDocenteService;

/** Administración de la asistencia de todos los docentes. */
@RestController
@RequestMapping("/api/v1/asistencias-docentes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AsistenciaDocenteController {

  private final AsistenciaDocenteService asistencias;

  public AsistenciaDocenteController(AsistenciaDocenteService asistencias) {
    this.asistencias = asistencias;
  }

  @PostMapping
  public ResponseEntity<AsistenciaDocenteResponse> registrar(
      @Valid @RequestBody AsistenciaDocenteCreateRequest request) {
    return ResponseEntity.status(201).body(asistencias.registrar(request));
  }

  @GetMapping
  public List<AsistenciaDocenteResponse> listar(
      @RequestParam(required = false) LocalDate fecha,
      @RequestParam(required = false) Integer docenteId) {
    return asistencias.listar(fecha, docenteId);
  }

  @GetMapping("/{asistenciaId}")
  public AsistenciaDocenteResponse obtener(@PathVariable Integer asistenciaId) {
    return asistencias.obtener(asistenciaId);
  }

  @PatchMapping("/{asistenciaId}")
  public AsistenciaDocenteResponse ajustar(
      @PathVariable Integer asistenciaId,
      @Valid @RequestBody AsistenciaDocenteAjusteRequest request) {
    return asistencias.ajustar(asistenciaId, request);
  }
}
