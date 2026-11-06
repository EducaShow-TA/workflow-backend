package pe.edu.pucp.colegio.common.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteResponse;
import pe.edu.pucp.colegio.common.service.AsistenciaDocenteService;

/**
 * Marcaje de entrada y salida del docente que inicia sesión, para acreditar el cumplimiento de su
 * jornada. La fecha y la hora las pone el servidor; el cliente no envía cuerpo.
 */
@RestController
@RequestMapping("/api/v1/docentes/mis-asistencias")
@PreAuthorize("hasRole('DOCENTE')")
public class MiAsistenciaDocenteController {

  private final AsistenciaDocenteService asistencias;

  public MiAsistenciaDocenteController(AsistenciaDocenteService asistencias) {
    this.asistencias = asistencias;
  }

  @GetMapping
  public List<AsistenciaDocenteResponse> listar(
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      Authentication authentication) {
    return asistencias.listarPropias(authentication.getName(), desde, hasta);
  }

  @PostMapping("/entrada")
  public ResponseEntity<AsistenciaDocenteResponse> marcarEntrada(Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(asistencias.marcarEntrada(authentication.getName()));
  }

  @PostMapping("/salida")
  public AsistenciaDocenteResponse marcarSalida(Authentication authentication) {
    return asistencias.marcarSalida(authentication.getName());
  }
}
