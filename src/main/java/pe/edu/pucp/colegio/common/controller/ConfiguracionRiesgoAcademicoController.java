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
import pe.edu.pucp.colegio.common.model.ConfiguracionRiesgoAcademico;
import pe.edu.pucp.colegio.common.repository.ConfiguracionRiesgoAcademicoRepository;

@RestController
@RequestMapping("/api/v1/configuracion/riesgo-academico")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracionRiesgoAcademicoController {

  private static final int ID_CONFIGURACION = 1;

  private final ConfiguracionRiesgoAcademicoRepository configuracion;

  public ConfiguracionRiesgoAcademicoController(
      ConfiguracionRiesgoAcademicoRepository configuracion) {
    this.configuracion = configuracion;
  }

  @GetMapping
  public UmbralRiesgoResponse obtener() {
    return new UmbralRiesgoResponse(
        configuracion.findById(ID_CONFIGURACION).orElseThrow().getUmbral());
  }

  @PatchMapping
  public UmbralRiesgoResponse actualizar(
      @Valid @RequestBody ActualizarUmbralRiesgoRequest request) {
    ConfiguracionRiesgoAcademico registro = configuracion.findById(ID_CONFIGURACION).orElseThrow();
    registro.setUmbral(request.umbral());
    return new UmbralRiesgoResponse(configuracion.save(registro).getUmbral());
  }
}
