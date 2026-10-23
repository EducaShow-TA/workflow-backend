package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaResponse;
import pe.edu.pucp.colegio.common.service.PeriodoMatriculaService;

@RestController
@RequestMapping("/api/v1/periodos-matricula")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PeriodoMatriculaController {

  private final PeriodoMatriculaService periodos;

  public PeriodoMatriculaController(PeriodoMatriculaService periodos) {
    this.periodos = periodos;
  }

  @PostMapping
  public ResponseEntity<PeriodoMatriculaResponse> crear(
      @Valid @RequestBody PeriodoMatriculaRequest request) {
    return ResponseEntity.status(201).body(periodos.crear(request));
  }

  @GetMapping
  public List<PeriodoMatriculaResponse> listar() {
    return periodos.listar();
  }

  @GetMapping("/{id}")
  public PeriodoMatriculaResponse buscar(@PathVariable Integer id) {
    return periodos.buscar(id);
  }

  @PutMapping("/{id}")
  public PeriodoMatriculaResponse actualizar(
      @PathVariable Integer id, @Valid @RequestBody PeriodoMatriculaRequest request) {
    return periodos.actualizar(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
    periodos.desactivar(id);
    return ResponseEntity.noContent().build();
  }
}
