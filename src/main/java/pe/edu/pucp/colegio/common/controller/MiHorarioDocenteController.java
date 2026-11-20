package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.HorarioDocenteResponse;
import pe.edu.pucp.colegio.common.dto.SesionHorarioDocenteRequest;
import pe.edu.pucp.colegio.common.dto.SesionHorarioResponse;
import pe.edu.pucp.colegio.common.service.MiHorarioDocenteService;

/** Horario semanal del docente que inició sesión. */
@RestController
@RequestMapping("/api/v1/docentes/mi-horario")
@PreAuthorize("hasRole('DOCENTE')")
public class MiHorarioDocenteController {

  private final MiHorarioDocenteService horario;

  public MiHorarioDocenteController(MiHorarioDocenteService horario) {
    this.horario = horario;
  }

  @GetMapping
  public List<HorarioDocenteResponse> listar(
      @RequestParam Integer anio, Authentication authentication) {
    return horario.listar(authentication.getName(), anio);
  }

  @PostMapping("/{horarioId}/sesiones")
  public ResponseEntity<SesionHorarioResponse> agregar(
      @PathVariable Integer horarioId,
      @Valid @RequestBody SesionHorarioDocenteRequest request,
      Authentication authentication) {
    return ResponseEntity.status(201)
        .body(horario.agregar(authentication.getName(), horarioId, request));
  }

  @PutMapping("/sesiones/{sesionId}")
  public SesionHorarioResponse actualizar(
      @PathVariable Integer sesionId,
      @Valid @RequestBody SesionHorarioDocenteRequest request,
      Authentication authentication) {
    return horario.actualizar(authentication.getName(), sesionId, request);
  }

  @DeleteMapping("/sesiones/{sesionId}")
  public ResponseEntity<Void> eliminar(
      @PathVariable Integer sesionId, Authentication authentication) {
    horario.eliminar(authentication.getName(), sesionId);
    return ResponseEntity.noContent().build();
  }
}
