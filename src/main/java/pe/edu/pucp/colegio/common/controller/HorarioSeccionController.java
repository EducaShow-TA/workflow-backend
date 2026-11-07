package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.HorarioSeccionCreateRequest;
import pe.edu.pucp.colegio.common.dto.HorarioSeccionResponse;
import pe.edu.pucp.colegio.common.dto.SesionHorarioRequest;
import pe.edu.pucp.colegio.common.dto.SesionHorarioResponse;
import pe.edu.pucp.colegio.common.dto.ValidacionSesionResponse;
import pe.edu.pucp.colegio.common.service.HorarioSeccionService;

/** Horario semanal de una sección: creación, grilla y sesiones de clase. */
@RestController
@RequestMapping("/api/v1/horarios/secciones")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class HorarioSeccionController {

  private final HorarioSeccionService horarios;

  public HorarioSeccionController(HorarioSeccionService horarios) {
    this.horarios = horarios;
  }

  @PostMapping
  public ResponseEntity<HorarioSeccionResponse> crear(
          @Valid @RequestBody HorarioSeccionCreateRequest request) {
    HorarioSeccionResponse creado = horarios.crear(request);
    return ResponseEntity.created(URI.create("/api/v1/horarios/secciones/" + creado.id()))
            .body(creado);
  }

  @GetMapping
  public HorarioSeccionResponse obtenerPorSeccion(
          @RequestParam Integer seccionId, @RequestParam Integer anio) {
    return horarios.obtenerPorSeccion(seccionId, anio);
  }

  @GetMapping("/{id}")
  public HorarioSeccionResponse obtener(@PathVariable Integer id) {
    return horarios.obtener(id);
  }

  @PostMapping("/{id}/sesiones")
  public ResponseEntity<SesionHorarioResponse> agregarSesion(
          @PathVariable Integer id, @Valid @RequestBody SesionHorarioRequest request) {
    SesionHorarioResponse creada = horarios.agregarSesion(id, request);
    return ResponseEntity.created(
                    URI.create("/api/v1/horarios/secciones/" + id + "/sesiones/" + creada.id()))
            .body(creada);
  }

  @PostMapping("/{id}/sesiones/validar")
  public ValidacionSesionResponse validarSesion(
          @PathVariable Integer id,
          @RequestParam(required = false) Integer excluirSesionId,
          @Valid @RequestBody SesionHorarioRequest request) {
    return horarios.validarSesion(id, request, excluirSesionId);
  }

  @PutMapping("/{id}/sesiones/{sesionId}")
  public SesionHorarioResponse actualizarSesion(
          @PathVariable Integer id,
          @PathVariable Integer sesionId,
          @Valid @RequestBody SesionHorarioRequest request) {
    return horarios.actualizarSesion(id, sesionId, request);
  }

  @DeleteMapping("/{id}/sesiones/{sesionId}")
  public ResponseEntity<Void> eliminarSesion(
          @PathVariable Integer id, @PathVariable Integer sesionId) {
    horarios.eliminarSesion(id, sesionId);
    return ResponseEntity.noContent().build();
  }
}