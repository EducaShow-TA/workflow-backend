package pe.edu.pucp.colegio.common.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.ReporteAnualNotasResponse;
import pe.edu.pucp.colegio.common.service.ReporteAnualNotasService;

@RestController
@RequestMapping("/api/v1/estudiantes/{estudianteId}/reporte-anual")
public class ReporteAnualNotasController {

  private final ReporteAnualNotasService reportes;

  public ReporteAnualNotasController(ReporteAnualNotasService reportes) {
    this.reportes = reportes;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'DOCENTE', 'APODERADO', 'ALUMNO')")
  public ReporteAnualNotasResponse generar(
      @PathVariable Integer estudianteId,
      @RequestParam Integer periodoId,
      Authentication authentication) {
    return reportes.generar(estudianteId, periodoId, authentication.getName());
  }
}
