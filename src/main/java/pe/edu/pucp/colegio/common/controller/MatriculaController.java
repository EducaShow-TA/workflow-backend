package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.MatriculaCreateRequest;
import pe.edu.pucp.colegio.common.dto.MatriculaResponse;
import pe.edu.pucp.colegio.common.service.MatriculaService;

/** Expone las operaciones de matrícula de estudiantes en periodos académicos. */
@RestController
@RequestMapping("/api/v1/periodos/{periodoId}/matriculas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class MatriculaController {

  private final MatriculaService matriculas;

  public MatriculaController(MatriculaService matriculas) {
    this.matriculas = matriculas;
  }

  @PostMapping
  public ResponseEntity<MatriculaResponse> crear(
      @PathVariable Integer periodoId, @Valid @RequestBody MatriculaCreateRequest request) {
    return ResponseEntity.status(201).body(matriculas.crear(periodoId, request));
  }

  @PatchMapping("/{estudianteId}/confirmar")
  public ResponseEntity<MatriculaResponse> confirmar(
      @PathVariable Integer periodoId, @PathVariable Integer estudianteId) {
    return ResponseEntity.ok(matriculas.confirmar(periodoId, estudianteId));
  }

  @DeleteMapping("/{estudianteId}")
  public ResponseEntity<Void> eliminar(
      @PathVariable Integer periodoId, @PathVariable Integer estudianteId) {
    matriculas.eliminar(periodoId, estudianteId);
    return ResponseEntity.noContent().build();
  }
}
