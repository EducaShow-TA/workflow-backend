package pe.edu.pucp.colegio.common.controller;

import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.dto.CargaMasivaAsistenciaResultadoDTO;
import pe.edu.pucp.colegio.common.service.CargaMasivaAsistenciaDocenteService;
import pe.edu.pucp.colegio.common.validation.ArchivoCsv;

/** Carga masiva de asistencia docente desde CSV (US-2.5). Solo el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/v1/asistencias-docentes/carga-masiva")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CargaMasivaAsistenciaDocenteController {

  private static final String PLANTILLA =
      """
      dni_docente,fecha,hora_entrada,hora_salida,estado,motivo
      10555666,2026-11-18,07:55,15:30,,
      10555667,2026-11-18,08:20,15:30,TARDANZA,Transporte público
      10555668,2026-11-18,,,INASISTENCIA,Descanso médico
      """;

  private final CargaMasivaAsistenciaDocenteService servicio;

  public CargaMasivaAsistenciaDocenteController(CargaMasivaAsistenciaDocenteService servicio) {
    this.servicio = servicio;
  }

  /**
   * Con {@code soloValidar=true} informa los errores sin guardar. Si alguna fila tiene errores
   * responde 422 con el detalle y no guarda nada.
   */
  @PostMapping
  public ResponseEntity<CargaMasivaAsistenciaResultadoDTO> cargar(
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      @RequestParam(value = "soloValidar", defaultValue = "false") boolean soloValidar,
      Authentication authentication)
      throws IOException {
    CargaMasivaAsistenciaResultadoDTO resultado =
        servicio.cargar(ArchivoCsv.leerContenido(archivo), soloValidar, authentication.getName());
    HttpStatus estado =
        resultado.errores().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(resultado);
  }

  /** Descarga un CSV de ejemplo con las columnas esperadas. */
  @GetMapping("/plantilla")
  public ResponseEntity<String> plantilla() {
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=plantilla-asistencia-docentes.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(PLANTILLA);
  }
}
