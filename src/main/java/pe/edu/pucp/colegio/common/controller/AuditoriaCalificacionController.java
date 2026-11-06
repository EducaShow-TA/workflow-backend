package pe.edu.pucp.colegio.common.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.audit.TipoCalificacionAuditoria;
import pe.edu.pucp.colegio.common.dto.AuditoriaCalificacionResponse;
import pe.edu.pucp.colegio.common.service.AuditoriaCalificacionService;

@RestController
@RequestMapping("/api/v1/logs/calificaciones")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaCalificacionController {

  private final AuditoriaCalificacionService auditoria;

  public AuditoriaCalificacionController(AuditoriaCalificacionService auditoria) {
    this.auditoria = auditoria;
  }

  @GetMapping
  public PagedModel<AuditoriaCalificacionResponse> listar(
      @RequestParam(required = false) Integer periodoId,
      @RequestParam(required = false) Integer matriculaId,
      @RequestParam(required = false) Integer estudianteId,
      @RequestParam(required = false) String competenciaCodigo,
      @RequestParam(required = false) TipoCalificacionAuditoria tipo,
      @PageableDefault(
              size = 20,
              sort = {"fechaHora", "id"},
              direction = Sort.Direction.DESC)
          Pageable pageable) {
    return new PagedModel<>(
        auditoria.listar(
            periodoId,
            matriculaId,
            estudianteId,
            competenciaCodigo,
            tipo == null ? null : tipo.name(),
            pageable));
  }
}
