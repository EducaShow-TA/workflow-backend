package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.EstudianteVinculadoResponse;
import pe.edu.pucp.colegio.common.service.EstudiantesApoderadoService;

/** Opciones de consulta de libreta limitadas a los hijos del apoderado autenticado. */
@RestController
@RequestMapping("/api/v1/apoderados/mis-estudiantes")
@PreAuthorize("hasRole('APODERADO')")
public class EstudiantesApoderadoController {

  private final EstudiantesApoderadoService estudiantes;

  public EstudiantesApoderadoController(EstudiantesApoderadoService estudiantes) {
    this.estudiantes = estudiantes;
  }

  @GetMapping
  public List<EstudianteVinculadoResponse> listar(Authentication authentication) {
    return estudiantes.listar(authentication.getName());
  }
}
