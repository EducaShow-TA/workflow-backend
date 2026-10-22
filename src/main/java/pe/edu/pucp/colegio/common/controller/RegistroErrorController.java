package pe.edu.pucp.colegio.common.controller;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.audit.NivelLog;
import pe.edu.pucp.colegio.common.audit.RegistroErrorService;
import pe.edu.pucp.colegio.common.dto.RegistroErrorResponseDTO;

/** Consulta de registros de error para soporte técnico (US-5.5). Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/v1/logs/errores")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RegistroErrorController {

  private final RegistroErrorService servicio;

  public RegistroErrorController(RegistroErrorService servicio) {
    this.servicio = servicio;
  }

  @GetMapping
  public PagedModel<RegistroErrorResponseDTO> listar(
      @RequestParam(required = false) NivelLog nivel,
      @RequestParam(required = false) String modulo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return new PagedModel<>(
        servicio
            .consultar(nivel, modulo, desde, hasta, pageable)
            .map(RegistroErrorResponseDTO::resumen));
  }

  @GetMapping("/{id}")
  public RegistroErrorResponseDTO detalle(@PathVariable Integer id) {
    return RegistroErrorResponseDTO.detalle(servicio.obtener(id));
  }
}
