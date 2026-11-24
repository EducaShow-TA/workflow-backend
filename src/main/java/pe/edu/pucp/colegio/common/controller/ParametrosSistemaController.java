package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.EstadoSistemaResponse;
import pe.edu.pucp.colegio.common.dto.FechaSistemaRequest;
import pe.edu.pucp.colegio.common.dto.PagoEnLineaRequest;
import pe.edu.pucp.colegio.common.dto.ParametrosSistemaResponse;
import pe.edu.pucp.colegio.common.service.ParametrosSistemaService;

@RestController
@RequestMapping("/api/v1/parametros-sistema")
public class ParametrosSistemaController {

  private final ParametrosSistemaService parametros;

  public ParametrosSistemaController(ParametrosSistemaService parametros) {
    this.parametros = parametros;
  }

  @GetMapping("/estado")
  public EstadoSistemaResponse estado() {
    return parametros.estado();
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ParametrosSistemaResponse obtener() {
    return parametros.obtener();
  }

  @PutMapping("/fecha")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ParametrosSistemaResponse simularFecha(
      @Valid @RequestBody FechaSistemaRequest request, Authentication authentication) {
    return parametros.simularFecha(request, authentication.getName());
  }

  @DeleteMapping("/fecha")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ParametrosSistemaResponse volverAFechaReal(Authentication authentication) {
    return parametros.volverAFechaReal(authentication.getName());
  }

  @PutMapping("/pago-en-linea")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public ParametrosSistemaResponse configurarPagoEnLinea(
      @Valid @RequestBody PagoEnLineaRequest request, Authentication authentication) {
    return parametros.configurarPagoEnLinea(request.activo(), authentication.getName());
  }
}
