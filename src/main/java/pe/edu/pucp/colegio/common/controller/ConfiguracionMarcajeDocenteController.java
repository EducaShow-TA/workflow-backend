package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.ConfiguracionMarcajeDocenteRequest;
import pe.edu.pucp.colegio.common.dto.ConfiguracionMarcajeDocenteResponse;
import pe.edu.pucp.colegio.common.service.ConfiguracionMarcajeDocenteService;

/** Parámetros globales del marcado de asistencia de los docentes. */
@RestController
@RequestMapping("/api/v1/configuracion/marcaje-docente")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracionMarcajeDocenteController {

  private final ConfiguracionMarcajeDocenteService configuracion;

  public ConfiguracionMarcajeDocenteController(ConfiguracionMarcajeDocenteService configuracion) {
    this.configuracion = configuracion;
  }

  @GetMapping
  public ConfiguracionMarcajeDocenteResponse obtener() {
    return configuracion.obtener();
  }

  @PatchMapping
  public ConfiguracionMarcajeDocenteResponse actualizar(
      @Valid @RequestBody ConfiguracionMarcajeDocenteRequest request) {
    return configuracion.actualizar(request);
  }
}
