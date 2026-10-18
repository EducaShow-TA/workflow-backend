package pe.edu.pucp.colegio.seguridad.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.colegio.seguridad.dto.CrearCuentaRequestDTO;
import pe.edu.pucp.colegio.seguridad.dto.CuentaResponseDTO;
import pe.edu.pucp.colegio.seguridad.service.CuentaUsuarioService;

/** Endpoints de cuentas de usuario. Solo el rol ADMINISTRADOR puede usarlos. */
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

  @GetMapping
  public ResponseEntity<List<CuentaResponseDTO>> listar() {
    return ResponseEntity.ok(servicio.listar());
  }
}
