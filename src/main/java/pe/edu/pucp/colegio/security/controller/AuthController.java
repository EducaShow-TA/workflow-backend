package pe.edu.pucp.colegio.security.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.colegio.security.dto.*;
import pe.edu.pucp.colegio.security.service.AuthService;
import pe.edu.pucp.colegio.security.service.RecuperacionPasswordService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService auth;
  private final RecuperacionPasswordService recuperacion;

  public AuthController(AuthService auth, RecuperacionPasswordService recuperacion) {
    this.auth = auth;
    this.recuperacion = recuperacion;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    return ResponseEntity.ok(auth.login(request));
  }

  @GetMapping("/me")
  public ResponseEntity<UsuarioPerfilDTO> me(Authentication authentication) {
    return ResponseEntity.ok(auth.currentUser(authentication.getName()));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Map<String, String>> solicitarRecuperacion(
      @Valid @RequestBody SolicitarRecuperacionPasswordRequestDTO request) {

    recuperacion.solicitarRecuperacion(request.username());

    return ResponseEntity.ok(
        Map.of(
            "message",
            "Si la cuenta existe, recibirás instrucciones para restablecer tu contraseña."));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Map<String, String>> restablecerPassword(
      @Valid @RequestBody RestablecerPasswordRequestDTO request) {

    recuperacion.restablecerPassword(request.token(), request.password());

    return ResponseEntity.ok(Map.of("message", "La contraseña se ha restablecido correctamente."));
  }
}
