package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.EstudianteCreateRequest;
import pe.edu.pucp.colegio.common.dto.EstudianteListadoResponse;
import pe.edu.pucp.colegio.common.dto.EstudianteResponse;
import pe.edu.pucp.colegio.common.service.EstudianteService;

@RestController
@RequestMapping("/api/v1/estudiantes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class EstudianteController {

  private final EstudianteService estudiantes;

  public EstudianteController(EstudianteService estudiantes) {
    this.estudiantes = estudiantes;
  }

  @PostMapping
  public ResponseEntity<EstudianteResponse> crear(
      @Valid @RequestBody EstudianteCreateRequest request) {
    return ResponseEntity.status(201).body(estudiantes.crear(request));
  }

  @GetMapping
  public Page<EstudianteListadoResponse> listar(
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String dni,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return estudiantes.listar(nombre, dni, gradoId, seccionId, page, size);
  }

  @PostMapping("/{cuentaUsuarioId}/asignar")
  public ResponseEntity<EstudianteResponse> asignarCuentaExistente(
      @PathVariable Integer cuentaUsuarioId) {
    return ResponseEntity.ok(estudiantes.asignarCuentaExistente(cuentaUsuarioId));
  }
}
