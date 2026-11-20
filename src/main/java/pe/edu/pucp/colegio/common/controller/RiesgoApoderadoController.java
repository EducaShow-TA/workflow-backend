package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AlertaRiesgoApoderadoResponse;
import pe.edu.pucp.colegio.common.service.RiesgoApoderadoService;

@RestController
@RequestMapping("/api/v1/apoderados/alertas-riesgo-academico")
@PreAuthorize("hasRole('APODERADO')")
public class RiesgoApoderadoController {
  private final RiesgoApoderadoService riesgo;

  public RiesgoApoderadoController(RiesgoApoderadoService riesgo) {
    this.riesgo = riesgo;
  }

  @GetMapping
  public List<AlertaRiesgoApoderadoResponse> listar(Authentication authentication) {
    return riesgo.listar(authentication.getName());
  }
}
