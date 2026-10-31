package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.AperturaAnhioRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoCreateRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoResponse;
import pe.edu.pucp.colegio.common.dto.VacantesPeriodoResponse;
import pe.edu.pucp.colegio.common.service.PeriodoService;
import pe.edu.pucp.colegio.common.service.VacantesService;

/** Expone las operaciones de administración de periodos académicos. */
@RestController
@RequestMapping("/api/v1/periodos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class PeriodoController {

  private final VacantesService vacantes;
  private final PeriodoService periodos;

  public PeriodoController(VacantesService vacantes, PeriodoService periodos) {
    this.vacantes = vacantes;
    this.periodos = periodos;
  }

  @PostMapping
  public ResponseEntity<PeriodoResponse> crear(@Valid @RequestBody PeriodoCreateRequest request) {
    return ResponseEntity.status(201).body(periodos.crear(request));
  }

  @GetMapping
  public List<VacantesPeriodoResponse> listar(
      @RequestParam(required = false) Integer anhio,
      @RequestParam(required = false) Integer gradoId,
      @RequestParam(required = false) Integer seccionId) {
    return vacantes.listar(anhio, gradoId, seccionId);
  }

  @PostMapping("/anhio")
  public ResponseEntity<List<VacantesPeriodoResponse>> abrirAnhio(
      @Valid @RequestBody AperturaAnhioRequest request) {
    return ResponseEntity.status(201).body(periodos.abrirAnhio(request));
  }

  @org.springframework.web.bind.annotation.PutMapping("/anhio")
  public List<VacantesPeriodoResponse> actualizarAnhio(@Valid @RequestBody AperturaAnhioRequest request) {
    return periodos.actualizarAnhio(request);
  }

  @GetMapping("/{periodoId}/vacantes")
  public VacantesPeriodoResponse consultarVacantes(@PathVariable Integer periodoId) {
    return vacantes.consultar(periodoId);
  }
}
