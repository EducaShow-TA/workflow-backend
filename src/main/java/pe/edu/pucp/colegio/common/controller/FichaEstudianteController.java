package pe.edu.pucp.colegio.common.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.EstudianteVinculadoResponse;
import pe.edu.pucp.colegio.common.service.FichaEstudianteService;

/** Datos del propio estudiante para el portal del alumno. */
@RestController
@RequestMapping("/api/v1/estudiantes/mi-ficha")
@PreAuthorize("hasRole('ALUMNO')")
public class FichaEstudianteController {

  private final FichaEstudianteService fichas;

  public FichaEstudianteController(FichaEstudianteService fichas) {
    this.fichas = fichas;
  }

  @GetMapping
  public EstudianteVinculadoResponse obtener(Authentication authentication) {
    return fichas.obtener(authentication.getName());
  }
}
