package pe.edu.pucp.colegio.security.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.CuentaTemporalResponse;
import pe.edu.pucp.colegio.security.dto.ActualizarCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CambiarEstadoCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CambiarRolCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CrearCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CrearDocenteRequestDTO;
import pe.edu.pucp.colegio.security.dto.CuentaResponseDTO;
import pe.edu.pucp.colegio.security.service.CuentaUsuarioService;

/** Expone las operaciones administrativas de gestión de cuentas de usuario. */
@RestController
@RequestMapping("/api/v1/cuentas-usuario")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CuentaUsuarioController {

  private final CuentaUsuarioService servicio;

  public CuentaUsuarioController(CuentaUsuarioService servicio) {
    this.servicio = servicio;
  }

  @PostMapping
  public ResponseEntity<CuentaResponseDTO> crear(
      @Valid @RequestBody CrearCuentaRequestDTO request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(servicio.crear(request, authentication.getName()));
  }

  @PostMapping("/docentes")
  public ResponseEntity<CuentaTemporalResponse> crearDocente(
      @Valid @RequestBody CrearDocenteRequestDTO request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .cacheControl(CacheControl.noStore())
        .body(servicio.crearDocente(request, authentication.getName()));
  }

  @GetMapping
  public ResponseEntity<List<CuentaResponseDTO>> listar() {
    return ResponseEntity.ok(servicio.listar());
  }

  @PutMapping("/{id}")
  public ResponseEntity<CuentaResponseDTO> actualizar(
      @PathVariable Integer id,
      @Valid @RequestBody ActualizarCuentaRequestDTO request,
      Authentication authentication) {
    return ResponseEntity.ok(servicio.actualizar(id, request, authentication.getName()));
  }

  @PatchMapping("/{id}/estado")
  public ResponseEntity<CuentaResponseDTO> cambiarEstado(
      @PathVariable Integer id,
      @Valid @RequestBody CambiarEstadoCuentaRequestDTO request,
      Authentication authentication) {
    return ResponseEntity.ok(servicio.cambiarEstado(id, request, authentication.getName()));
  }

  @PatchMapping("/{id}/rol")
  public ResponseEntity<CuentaResponseDTO> cambiarRol(
      @PathVariable Integer id,
      @Valid @RequestBody CambiarRolCuentaRequestDTO request,
      Authentication authentication) {
    return ResponseEntity.ok(servicio.cambiarRol(id, request, authentication.getName()));
  }
}
