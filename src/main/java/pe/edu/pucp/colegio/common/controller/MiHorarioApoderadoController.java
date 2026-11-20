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

/** Horario semanal de un estudiante vinculado al apoderado autenticado. */
@RestController
@RequestMapping("/api/v1/apoderados/mi-horario")
@PreAuthorize("hasRole('APODERADO')")
public class MiHorarioApoderadoController {

  private final MiHorarioEstudianteService horario;

  public MiHorarioApoderadoController(MiHorarioEstudianteService horario) {
    this.horario = horario;
  }

  @GetMapping
  public List<HorarioEstudianteResponse> listar(
      @RequestParam Integer estudianteId,
      @RequestParam Integer anio,
      Authentication authentication) {
    return horario.listarParaApoderado(authentication.getName(), estudianteId, anio);
  }
}
