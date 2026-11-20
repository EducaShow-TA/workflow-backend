package pe.edu.pucp.colegio.common.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AuditoriaHorarioResponse;
import pe.edu.pucp.colegio.common.service.AuditoriaHorarioService;

@RestController
@RequestMapping("/api/v1/horarios/auditoria")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaHorarioController {
  private final AuditoriaHorarioService auditoria;

  public AuditoriaHorarioController(AuditoriaHorarioService auditoria) {
    this.auditoria = auditoria;
  }

  @GetMapping
  public List<AuditoriaHorarioResponse> listar(
      @RequestParam(required = false) Integer horarioId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta) {
    LocalDate fin = hasta == null ? LocalDate.now() : hasta;
    LocalDate inicio = desde == null ? fin.minusMonths(3) : desde;
    if (inicio.isAfter(fin))
      throw new IllegalArgumentException("El inicio no puede ser posterior al fin");
    return auditoria.listar(horarioId, inicio.atStartOfDay(), fin.atTime(LocalTime.MAX));
  }
}
