package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.PeriodoCreateRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoResponse;
import pe.edu.pucp.colegio.common.dto.VacantesPeriodoResponse;
import pe.edu.pucp.colegio.common.service.PeriodoService;
import pe.edu.pucp.colegio.common.service.VacantesService;

@RestController
@RequestMapping("/api/v1/periodos")
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

  @GetMapping("/{periodoId}/vacantes")
  public VacantesPeriodoResponse consultarVacantes(@PathVariable Integer periodoId) {
    return vacantes.consultar(periodoId);
  }
}
