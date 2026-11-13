package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.DiaNoLectivoRequest;
import pe.edu.pucp.colegio.common.dto.DiaNoLectivoResponse;
import pe.edu.pucp.colegio.common.service.GeneracionSesionClaseService;

/** Administración de feriados, recesos y otros días sin clases. */
@RestController
@RequestMapping("/api/v1/calendario-escolar/dias-no-lectivos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CalendarioEscolarController {

  private final GeneracionSesionClaseService calendario;

  public CalendarioEscolarController(GeneracionSesionClaseService calendario) {
    this.calendario = calendario;
  }

  @PostMapping
  public ResponseEntity<DiaNoLectivoResponse> crear(
      @Valid @RequestBody DiaNoLectivoRequest request) {
    return ResponseEntity.status(201).body(calendario.crearDiaNoLectivo(request));
  }

  @GetMapping
  public List<DiaNoLectivoResponse> listar(
      @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
    return calendario.listarDiasNoLectivos(desde, hasta);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
    calendario.eliminarDiaNoLectivo(id);
    return ResponseEntity.noContent().build();
  }
}
