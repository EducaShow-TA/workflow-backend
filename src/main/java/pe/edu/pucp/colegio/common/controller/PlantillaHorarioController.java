package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
import pe.edu.pucp.colegio.common.dto.CambioEstadoPlantillaRequest;
import pe.edu.pucp.colegio.common.dto.PlantillaHorarioRequest;
import pe.edu.pucp.colegio.common.dto.PlantillaHorarioResponse;
import pe.edu.pucp.colegio.common.service.PlantillaHorarioService;

@RestController
@RequestMapping("/api/v1/horarios/plantillas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PlantillaHorarioController {

  private final PlantillaHorarioService plantillas;

  public PlantillaHorarioController(PlantillaHorarioService plantillas) {
    this.plantillas = plantillas;
  }

  @GetMapping
  public List<PlantillaHorarioResponse> listar() {
    return plantillas.listar();
  }

  @GetMapping("/{id}")
  public PlantillaHorarioResponse obtener(@PathVariable Integer id) {
    return plantillas.obtener(id);
  }

  @PostMapping
  public ResponseEntity<PlantillaHorarioResponse> crear(
      @Valid @RequestBody PlantillaHorarioRequest request) {
    PlantillaHorarioResponse creada = plantillas.crear(request);
    return ResponseEntity.created(URI.create("/api/v1/horarios/plantillas/" + creada.id()))
        .body(creada);
  }

  @PutMapping("/{id}")
  public PlantillaHorarioResponse actualizar(
      @PathVariable Integer id, @Valid @RequestBody PlantillaHorarioRequest request) {
    return plantillas.actualizar(id, request);
  }

  @PatchMapping("/{id}/estado")
  public PlantillaHorarioResponse cambiarEstado(
      @PathVariable Integer id, @Valid @RequestBody CambioEstadoPlantillaRequest request) {
    return plantillas.cambiarEstado(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
    plantillas.desactivar(id);
    return ResponseEntity.noContent().build();
  }
}
