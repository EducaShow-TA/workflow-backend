package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.HorarioFamiliarResponse;
import pe.edu.pucp.colegio.common.service.HorarioFamiliarService;

/** US-4.7: acceso al horario de las propias secciones, nunca a secciones ajenas. */
@RestController
@RequestMapping("/api/v1/horarios/mi-seccion")
@PreAuthorize("hasAnyRole('ALUMNO', 'APODERADO')")
public class HorarioFamiliarController {

  private final HorarioFamiliarService horarios;

  public HorarioFamiliarController(HorarioFamiliarService horarios) {
    this.horarios = horarios;
  }

  @GetMapping
  public List<HorarioFamiliarResponse> listar(
      @RequestParam(required = false) Integer anio, Authentication authentication) {
    return horarios.listar(authentication.getName(), anio);
  }
}
