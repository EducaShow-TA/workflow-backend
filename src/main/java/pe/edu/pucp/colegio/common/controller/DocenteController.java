package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.DocenteResponse;
import pe.edu.pucp.colegio.common.service.DocenteService;

/** Consulta de docentes para asignarlos a sesiones de clase. */
@RestController
@RequestMapping("/api/v1/docentes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class DocenteController {

  private final DocenteService docentes;

  public DocenteController(DocenteService docentes) {
    this.docentes = docentes;
  }

  @GetMapping
  public List<DocenteResponse> listar() {
    return docentes.listar();
  }
}
