package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.EstudianteCreateRequest;
import pe.edu.pucp.colegio.common.dto.EstudianteResponse;
import pe.edu.pucp.colegio.common.service.EstudianteService;

@RestController
@RequestMapping("/api/v1/estudiantes")
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

  @PostMapping("/{cuentaUsuarioId}/asignar")
  public ResponseEntity<EstudianteResponse> asignarCuentaExistente(
      @PathVariable Integer cuentaUsuarioId) {
    return ResponseEntity.ok(estudiantes.asignarCuentaExistente(cuentaUsuarioId));
  }
}
