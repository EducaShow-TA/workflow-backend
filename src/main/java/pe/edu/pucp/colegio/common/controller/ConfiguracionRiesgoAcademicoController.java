package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.ActualizarUmbralRiesgoRequest;
import pe.edu.pucp.colegio.common.dto.UmbralRiesgoResponse;
import pe.edu.pucp.colegio.common.service.ConfiguracionRiesgoAcademicoService;

@RestController
@RequestMapping("/api/v1/configuracion/riesgo-academico")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracionRiesgoAcademicoController {

  private final ConfiguracionRiesgoAcademicoService configuracion;

  public ConfiguracionRiesgoAcademicoController(ConfiguracionRiesgoAcademicoService configuracion) {
    this.configuracion = configuracion;
  }

  @GetMapping
  public UmbralRiesgoResponse obtener() {
    return configuracion.obtener();
  }

  @PatchMapping
  public UmbralRiesgoResponse actualizar(
      @Valid @RequestBody ActualizarUmbralRiesgoRequest request) {
    return configuracion.actualizar(request);
  }
}
