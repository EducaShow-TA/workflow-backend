package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.SeccionCreateRequest;
import pe.edu.pucp.colegio.common.dto.SeccionResponse;
import pe.edu.pucp.colegio.common.dto.SeccionVacantesRequest;
import pe.edu.pucp.colegio.common.service.SeccionService;

/** Expone las operaciones de administración de secciones y sus vacantes. */
@RestController
@RequestMapping("/api/v1/secciones")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class SeccionController {

  private final SeccionService secciones;

  public SeccionController(SeccionService secciones) {
    this.secciones = secciones;
  }

  @GetMapping
  public List<SeccionResponse> listar() {
    return secciones.listar();
  }

  @PostMapping
  public ResponseEntity<SeccionResponse> crear(@Valid @RequestBody SeccionCreateRequest request) {
    SeccionResponse creada = secciones.crear(request);
    return ResponseEntity.created(URI.create("/api/v1/secciones/" + creada.id())).body(creada);
  }

  @PatchMapping("/{id}")
  public SeccionResponse actualizarVacantes(
      @PathVariable Integer id, @Valid @RequestBody SeccionVacantesRequest request) {
    return secciones.actualizarVacantes(id, request);
  }
}
