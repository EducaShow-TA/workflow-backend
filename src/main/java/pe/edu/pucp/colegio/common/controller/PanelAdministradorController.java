package pe.edu.pucp.colegio.common.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.PanelAdministradorResponse;
import pe.edu.pucp.colegio.common.service.PanelAdministradorService;

@RestController
@RequestMapping("/api/v1/panel/administrador")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PanelAdministradorController {

  private final PanelAdministradorService panel;

  public PanelAdministradorController(PanelAdministradorService panel) {
    this.panel = panel;
  }

  @GetMapping
  public PanelAdministradorResponse resumen() {
    return panel.resumen();
  }
}
