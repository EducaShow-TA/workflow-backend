package pe.edu.pucp.colegio.common.controller;

import java.io.IOException;
import java.util.stream.Collectors;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.dto.CargaMasivaAsistenciaResultadoDTO;
import pe.edu.pucp.colegio.common.dto.ListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.service.CargaMasivaAsistenciaEstudianteService;
import pe.edu.pucp.colegio.common.service.SesionClaseService;
import pe.edu.pucp.colegio.common.validation.ArchivoCsv;

/** Carga masiva de la asistencia de alumnos de una sesión desde CSV (US-2.2). */
@RestController
@RequestMapping("/api/v1/sesiones-clase/{sesionId}/asistencias/carga-masiva")
@PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
public class CargaMasivaAsistenciaEstudianteController {

  private final CargaMasivaAsistenciaEstudianteService servicio;
  private final SesionClaseService sesiones;

  public CargaMasivaAsistenciaEstudianteController(
      CargaMasivaAsistenciaEstudianteService servicio, SesionClaseService sesiones) {
    this.servicio = servicio;
    this.sesiones = sesiones;
  }

  /**
   * Con {@code soloValidar=true} informa los errores sin guardar. Si alguna fila tiene errores
   * responde 422 con el detalle y no guarda nada.
   */
  @PostMapping
  public ResponseEntity<CargaMasivaAsistenciaResultadoDTO> cargar(
      @PathVariable Integer sesionId,
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      @RequestParam(value = "soloValidar", defaultValue = "false") boolean soloValidar,
      Authentication authentication)
      throws IOException {
    CargaMasivaAsistenciaResultadoDTO resultado =
        servicio.cargar(
            sesionId, ArchivoCsv.leerContenido(archivo), soloValidar, acceso(authentication));
    HttpStatus estado =
        resultado.errores().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
    return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(resultado);
  }

  /** CSV con los alumnos matriculados de la sesión y la columna de estado vacía por completar. */
  @GetMapping("/plantilla")
  public ResponseEntity<String> plantilla(
      @PathVariable Integer sesionId, Authentication authentication) {
    ListaAsistenciaResponse lista =
        sesiones.obtenerListaParaCarga(sesionId, acceso(authentication));
    String filas =
        lista.alumnos().stream()
            .filter(alumno -> alumno.matriculado())
            .map(alumno -> alumno.dni() + "," + alumno.nombreCompleto().replace(',', ' ') + ",")
            .collect(Collectors.joining("\n"));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-asistencia.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body("dni_alumno,nombre,estado\n" + filas + "\n");
  }

  private static SesionClaseService.Acceso acceso(Authentication authentication) {
    boolean administrador =
        authentication.getAuthorities().stream()
            .anyMatch(autoridad -> "ROLE_ADMINISTRADOR".equals(autoridad.getAuthority()));
    return new SesionClaseService.Acceso(authentication.getName(), administrador);
  }
}
