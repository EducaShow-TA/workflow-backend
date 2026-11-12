package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AlumnoClaseResponse;
import pe.edu.pucp.colegio.common.dto.MiClaseResponse;
import pe.edu.pucp.colegio.common.dto.RiesgoAcademicoResponse;
import pe.edu.pucp.colegio.common.service.MisClasesService;

/** Clases y alumnos del docente que inicia sesión, para registrar calificaciones (HU 3.2). */
@RestController
@RequestMapping("/api/v1/docentes/mis-clases")
@PreAuthorize("hasRole('DOCENTE')")
public class MisClasesController {

  private final MisClasesService clases;

  public MisClasesController(MisClasesService clases) {
    this.clases = clases;
  }

  @GetMapping
  public List<MiClaseResponse> listar(Authentication authentication) {
    return clases.listar(authentication.getName());
  }

  @GetMapping("/{periodoId}/alumnos")
  public List<AlumnoClaseResponse> listarAlumnos(
      @PathVariable Integer periodoId, Authentication authentication) {
    return clases.listarAlumnos(authentication.getName(), periodoId);
  }

  @GetMapping("/{periodoId}/riesgo-academico")
  public RiesgoAcademicoResponse riesgoAcademico(
      @PathVariable Integer periodoId,
      @RequestParam String areaCodigo,
      @RequestParam(defaultValue = "2") int umbral,
      Authentication authentication) {
    return clases.riesgoAcademico(authentication.getName(), periodoId, areaCodigo, umbral);
  }
}
