package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.HorarioDocenteResponse;
import pe.edu.pucp.colegio.common.service.MiHorarioDocenteService;

/** Horario semanal del docente que inició sesión. */
@RestController
@RequestMapping("/api/v1/docentes/mi-horario")
@PreAuthorize("hasRole('DOCENTE')")
public class MiHorarioDocenteController {

  private final MiHorarioDocenteService horario;

  public MiHorarioDocenteController(MiHorarioDocenteService horario) {
    this.horario = horario;
  }

  @GetMapping
  public List<HorarioDocenteResponse> listar(
      @RequestParam Integer anio, Authentication authentication) {
    return horario.listar(authentication.getName(), anio);
  }
}
