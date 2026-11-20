package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.CambioEstadoCatalogoRequest;
import pe.edu.pucp.colegio.common.dto.CatalogoCompetenciaResponse;
import pe.edu.pucp.colegio.common.dto.CompetenciaRequest;
import pe.edu.pucp.colegio.common.dto.CompetenciaResponse;
import pe.edu.pucp.colegio.common.service.CatalogoService;

/** Expone las operaciones de consulta y gestión de competencias curriculares. */
@RestController
@RequestMapping("/api/v1/competencias")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CompetenciaController {

  private final CatalogoService catalogo;

  public CompetenciaController(CatalogoService catalogo) {
    this.catalogo = catalogo;
  }

  @GetMapping("/{codigo}")
  public ResponseEntity<CompetenciaResponse> obtener(@PathVariable String codigo) {
    return ResponseEntity.ok(catalogo.obtenerCompetencia(codigo));
  }

  @PostMapping
  public ResponseEntity<CompetenciaResponse> crear(@Valid @RequestBody CompetenciaRequest request) {
    CompetenciaResponse creada = catalogo.crearCompetencia(request);
    return ResponseEntity.created(URI.create("/api/v1/competencias/" + creada.codigo()))
        .body(creada);
  }

  @PutMapping("/{codigo}")
  public ResponseEntity<CompetenciaResponse> actualizar(
      @PathVariable String codigo, @Valid @RequestBody CompetenciaRequest request) {
    return ResponseEntity.ok(catalogo.actualizarCompetencia(codigo, request));
  }

  @PatchMapping("/{id}/estado")
  public CatalogoCompetenciaResponse cambiarEstado(
      @PathVariable Integer id, @Valid @RequestBody CambioEstadoCatalogoRequest request) {
    return catalogo.cambiarEstadoCompetencia(id, request);
  }

  @DeleteMapping("/{codigo}")
  public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
    catalogo.eliminarCompetencia(codigo);
    return ResponseEntity.noContent().build();
  }
}
