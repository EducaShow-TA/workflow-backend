package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.colegio.common.dto.AreaRequest;
import pe.edu.pucp.colegio.common.dto.AreaResponse;
import pe.edu.pucp.colegio.common.service.CatalogoService;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {

  private final CatalogoService catalogo;

  public AreaController(CatalogoService catalogo) {
    this.catalogo = catalogo;
  }

  @GetMapping("/{codigo}")
  public ResponseEntity<AreaResponse> obtener(@PathVariable String codigo) {
    return ResponseEntity.ok(catalogo.obtenerArea(codigo));
  }

  @PostMapping
  public ResponseEntity<AreaResponse> crear(@Valid @RequestBody AreaRequest request) {
    AreaResponse creada = catalogo.crearArea(request);
    return ResponseEntity.created(URI.create("/api/v1/areas/" + creada.codigo())).body(creada);
  }

  @PutMapping("/{codigo}")
  public ResponseEntity<AreaResponse> actualizar(
      @PathVariable String codigo, @Valid @RequestBody AreaRequest request) {
    return ResponseEntity.ok(catalogo.actualizarArea(codigo, request));
  }

  @DeleteMapping("/{codigo}")
  public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
    catalogo.eliminarArea(codigo);
    return ResponseEntity.noContent().build();
  }
}
