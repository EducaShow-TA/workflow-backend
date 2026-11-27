package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.DocenteResponse;
import pe.edu.pucp.colegio.common.service.DocenteService;
import pe.edu.pucp.colegio.common.service.HorarioSeccionService;

/** Consulta de docentes para asignarlos a sesiones de clase. */
@RestController
@RequestMapping("/api/v1/docentes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class DocenteController {

  private final DocenteService docentes;
  private final HorarioSeccionService horarios;

  public DocenteController(DocenteService docentes, HorarioSeccionService horarios) {
    this.docentes = docentes;
    this.horarios = horarios;
  }

  @GetMapping
  public List<DocenteResponse> listar() {
    return docentes.listar();
  }

  @GetMapping("/{docenteId}/disponibilidad")
  public List<HorarioSeccionService.DisponibilidadDocente> disponibilidad(
      @PathVariable Integer docenteId, @RequestParam Integer anio) {
    return horarios.disponibilidadDocente(docenteId, anio);
  }
}
