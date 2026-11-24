package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.BimestreResponse;
import pe.edu.pucp.colegio.common.dto.CierreBimestreResponse;
import pe.edu.pucp.colegio.common.service.CalendarioBimestralService;
import pe.edu.pucp.colegio.common.service.CierreBimestreService;

@RestController
@RequestMapping("/api/v1/bimestres")
public class BimestreController {

  private final CalendarioBimestralService calendario;
  private final CierreBimestreService cierre;

  public BimestreController(CalendarioBimestralService calendario, CierreBimestreService cierre) {
    this.calendario = calendario;
    this.cierre = cierre;
  }

  @GetMapping
  public List<BimestreResponse> listar(@RequestParam Integer anhio) {
    return calendario.listar(anhio);
  }

  @PostMapping("/{anhio}/{numero}/cierre")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public CierreBimestreResponse cerrar(
      @PathVariable Integer anhio, @PathVariable Integer numero, Authentication authentication) {
    return cierre.cerrar(anhio, numero, authentication.getName());
  }
}
