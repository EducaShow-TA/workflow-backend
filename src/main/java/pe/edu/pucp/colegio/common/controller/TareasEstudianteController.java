package pe.edu.pucp.colegio.common.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.dto.TareaEstudianteResponse;
import pe.edu.pucp.colegio.common.service.TareasEstudianteService;

/** Tareas del alumno autenticado y entrega de su archivo (US-3.9 y US-3.10). */
@RestController
@RequestMapping("/api/v1/estudiantes/mis-tareas")
@PreAuthorize("hasRole('ALUMNO')")
public class TareasEstudianteController {

  private final TareasEstudianteService service;

  public TareasEstudianteController(TareasEstudianteService service) {
    this.service = service;
  }

  @GetMapping
  public List<TareaEstudianteResponse> listar(Authentication authentication) {
    return service.listar(authentication.getName());
  }

  @GetMapping("/{tareaId}")
  public TareaEstudianteResponse obtener(
      @PathVariable Integer tareaId, Authentication authentication) {
    return service.obtener(authentication.getName(), tareaId);
  }

  @PostMapping(value = "/{tareaId}/entrega", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public TareaEstudianteResponse entregar(
      @PathVariable Integer tareaId,
      @RequestPart("archivo") MultipartFile archivo,
      Authentication authentication) {
    return service.entregar(authentication.getName(), tareaId, archivo);
  }

  @GetMapping("/{tareaId}/entrega/archivo")
  public ResponseEntity<Resource> descargar(
      @PathVariable Integer tareaId, Authentication authentication) {
    return descarga(service.archivo(authentication.getName(), tareaId));
  }

  static ResponseEntity<Resource> descarga(TareasEstudianteService.ArchivoEntrega archivo) {
    MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
    if (archivo.tipo() != null) {
      try {
        tipo = MediaType.parseMediaType(archivo.tipo());
      } catch (IllegalArgumentException ignored) {
        tipo = MediaType.APPLICATION_OCTET_STREAM;
      }
    }
    return ResponseEntity.ok()
        .contentType(tipo)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(archivo.nombre(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(archivo.recurso());
  }
}
