package pe.edu.pucp.colegio.common.controller;

import java.io.IOException;
import java.util.List;
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
import pe.edu.pucp.colegio.common.service.PlantillaCargaArchivoService;
import pe.edu.pucp.colegio.common.validation.ArchivoCsv;

/** Carga masiva de asistencia docente desde CSV (US-2.5). Solo el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/v1/asistencias-docentes/carga-masiva")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CargaMasivaAsistenciaDocenteController {

  private static final String PLANTILLA =
      """
      dni_docente,fecha,hora_entrada,hora_salida,estado,motivo
      """;

  private final CargaMasivaAsistenciaDocenteService servicio;
  private final PlantillaCargaArchivoService plantillas;

  public CargaMasivaAsistenciaDocenteController(
      CargaMasivaAsistenciaDocenteService servicio, PlantillaCargaArchivoService plantillas) {
    this.servicio = servicio;
    this.plantillas = plantillas;
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

  @GetMapping(value = "/plantilla", params = "formato=xlsx")
  public ResponseEntity<byte[]> plantillaExcel() {
    byte[] archivo =
        plantillas.generarExcel(
            "Asistencia de docentes",
            "Plantilla para la carga administrativa de marcajes",
            List.of("dni_docente", "fecha", "hora_entrada", "hora_salida", "estado", "motivo"),
            List.of(),
            List.of(
                "Completa una fila por docente y fecha. Usa el DNI del docente registrado.",
                "La fecha usa AAAA-MM-DD y las horas HH:mm.",
                "Estados: PRESENTE, TARDANZA o INASISTENCIA. Para PRESENTE puedes dejar estado vacío si completas las horas.",
                "Valida el archivo antes de guardar el lote."));
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=plantilla-asistencia-docentes.xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(archivo);
  }
}
