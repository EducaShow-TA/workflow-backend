package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.SituacionMatriculaResponse;
import pe.edu.pucp.colegio.common.service.AprobacionAnualService;

/** Situación de cada estudiante (nuevo, aprobó, repite, egresó) para matricularse en un año. */
@RestController
@RequestMapping("/api/v1/matriculas/situacion")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class SituacionMatriculaController {

  private final AprobacionAnualService aprobacion;

  public SituacionMatriculaController(AprobacionAnualService aprobacion) {
    this.aprobacion = aprobacion;
  }

  @GetMapping
  public List<SituacionMatriculaResponse> listar(@RequestParam int anhio) {
    return aprobacion.situaciones(anhio);
  }
}
