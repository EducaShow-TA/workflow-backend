package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.colegio.common.dto.AreaCatalogoCreateRequest;
import pe.edu.pucp.colegio.common.dto.AreaRequest;
import pe.edu.pucp.colegio.common.dto.AreaResponse;
import pe.edu.pucp.colegio.common.dto.CambioEstadoCatalogoRequest;
import pe.edu.pucp.colegio.common.dto.CatalogoAreaResponse;
import pe.edu.pucp.colegio.common.dto.CatalogoCompetenciaResponse;
import pe.edu.pucp.colegio.common.dto.CompetenciaNuevaRequest;
import pe.edu.pucp.colegio.common.service.CatalogoService;

@RestController
@RequestMapping("/api/v1/areas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AreaController {

  private final CatalogoService catalogo;

  public AreaController(CatalogoService catalogo) {
    this.catalogo = catalogo;
  }

  @GetMapping
  public List<CatalogoAreaResponse> listar() {
    return catalogo.listarAreas();
  }

  @GetMapping("/{codigo}")
  public ResponseEntity<AreaResponse> obtener(@PathVariable String codigo) {
    return ResponseEntity.ok(catalogo.obtenerArea(codigo));
  }

  @PostMapping
  public ResponseEntity<CatalogoAreaResponse> crear(
      @Valid @RequestBody AreaCatalogoCreateRequest request) {
    CatalogoAreaResponse creada = catalogo.crearArea(request);
    return ResponseEntity.created(URI.create("/api/v1/areas/" + creada.id())).body(creada);
  }

  @PutMapping("/{codigo}")
  public ResponseEntity<CatalogoAreaResponse> actualizar(
      @PathVariable String codigo, @Valid @RequestBody AreaRequest request) {
    return ResponseEntity.ok(catalogo.actualizarArea(codigo, request));
  }

  @PatchMapping("/{id}/estado")
  public CatalogoAreaResponse cambiarEstado(
      @PathVariable Integer id, @Valid @RequestBody CambioEstadoCatalogoRequest request) {
    return catalogo.cambiarEstadoArea(id, request);
  }

  @PostMapping("/{areaId}/competencias")
  public ResponseEntity<CatalogoCompetenciaResponse> crearCompetencia(
      @PathVariable Integer areaId, @Valid @RequestBody CompetenciaNuevaRequest request) {
    CatalogoCompetenciaResponse creada = catalogo.crearCompetenciaEnArea(areaId, request);
    return ResponseEntity.created(URI.create("/api/v1/competencias/" + creada.id())).body(creada);
  }

  @DeleteMapping("/{codigo}")
  public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
    catalogo.eliminarArea(codigo);
    return ResponseEntity.noContent().build();
  }
}
