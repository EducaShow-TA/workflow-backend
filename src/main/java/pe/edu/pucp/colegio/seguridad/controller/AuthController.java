package pe.edu.pucp.colegio.seguridad.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.colegio.seguridad.dto.*;
import pe.edu.pucp.colegio.seguridad.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    return ResponseEntity.ok(auth.login(request));
  }

  @GetMapping("/me")
  public ResponseEntity<UsuarioPerfilDTO> me(Authentication authentication) {
    return ResponseEntity.ok(auth.currentUser(authentication.getName()));
  }
}
