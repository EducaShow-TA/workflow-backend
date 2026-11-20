package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.HorarioEstudianteResponse;
import pe.edu.pucp.colegio.common.service.MiHorarioEstudianteService;

/** Horario semanal del alumno autenticado, según su matrícula confirmada. */
@RestController
@RequestMapping("/api/v1/estudiantes/mi-horario")
@PreAuthorize("hasRole('ALUMNO')")
public class MiHorarioEstudianteController {

  private final MiHorarioEstudianteService horario;

  public MiHorarioEstudianteController(MiHorarioEstudianteService horario) {
    this.horario = horario;
  }

  @GetMapping
  public List<HorarioEstudianteResponse> listar(
      @RequestParam Integer anio, Authentication authentication) {
    return horario.listar(authentication.getName(), anio);
  }
}
