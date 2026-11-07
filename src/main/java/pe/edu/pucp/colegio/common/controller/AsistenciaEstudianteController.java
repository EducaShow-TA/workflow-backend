package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.dto.AsistenciaHistorialResponse;
import pe.edu.pucp.colegio.common.dto.AsistenciaJustificacionResponse;
import pe.edu.pucp.colegio.common.service.AsistenciaHistorialService;
import pe.edu.pucp.colegio.common.service.AsistenciaJustificacionService;

/** Consulta el historial y gestiona documentos de justificación de alumnos. */
@Validated
@RestController
@RequestMapping("/api/v1/asistencias-estudiantes")
@PreAuthorize("hasAnyRole('ALUMNO', 'APODERADO', 'DOCENTE', 'ADMINISTRADOR')")
public class AsistenciaEstudianteController {

  private final AsistenciaHistorialService historial;
  private final AsistenciaJustificacionService justificaciones;

  public AsistenciaEstudianteController(
      AsistenciaHistorialService historial, AsistenciaJustificacionService justificaciones) {
    this.historial = historial;
    this.justificaciones = justificaciones;
  }

  @GetMapping("/{estudianteId}/historial")
  public List<AsistenciaHistorialResponse> consultarHistorial(
      @PathVariable Integer estudianteId,
      @RequestParam(required = false) LocalDate desde,
      @RequestParam(required = false) LocalDate hasta,
      Authentication authentication) {
    return historial.consultar(estudianteId, desde, hasta, authentication.getName());
  }

  @PatchMapping(
      value = "/{asistenciaId}/justificacion",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AsistenciaJustificacionResponse justificar(
      @PathVariable Integer asistenciaId,
      @RequestPart("motivo") @Size(max = 500) String motivo,
      @RequestPart("documento") MultipartFile documento,
      Authentication authentication) {
    return justificaciones.justificar(asistenciaId, motivo, documento, authentication.getName());
  }

  @GetMapping("/{asistenciaId}/justificacion/documento")
  public ResponseEntity<Resource> descargarDocumento(
      @PathVariable Integer asistenciaId, Authentication authentication) {
    var documento = justificaciones.descargar(asistenciaId, authentication.getName());
    MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
    if (documento.tipo() != null) {
      try {
        tipo = MediaType.parseMediaType(documento.tipo());
      } catch (IllegalArgumentException ignored) {
        // Si el tipo cargado no es válido, se entrega como binario.
      }
    }
    String nombre =
        documento.nombre() == null
            ? "justificacion"
            : documento.nombre().replaceAll("[\\r\\n\\\"]", "_");
    return ResponseEntity.ok()
        .contentType(tipo)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
        .body(documento.recurso());
  }
}
