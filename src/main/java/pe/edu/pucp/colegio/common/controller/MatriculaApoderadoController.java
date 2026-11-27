package pe.edu.pucp.colegio.common.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.dto.CompromisoPagoRequest;
import pe.edu.pucp.colegio.common.dto.MatriculaPendienteApoderadoResponse;
import pe.edu.pucp.colegio.common.dto.MatriculaResponse;
import pe.edu.pucp.colegio.common.service.MatriculaApoderadoService;

@RestController
@RequestMapping("/api/v1/apoderados/matriculas")
@PreAuthorize("hasRole('APODERADO')")
public class MatriculaApoderadoController {

  private final MatriculaApoderadoService matriculas;

  public MatriculaApoderadoController(MatriculaApoderadoService matriculas) {
    this.matriculas = matriculas;
  }

  @GetMapping("/pendientes")
  public List<MatriculaPendienteApoderadoResponse> listarPendientes(Authentication authentication) {
    return matriculas.listarPendientes(authentication.getName());
  }

  @PatchMapping("/{periodoId}/{estudianteId}/confirmar")
  public MatriculaResponse confirmar(
      @PathVariable Integer periodoId,
      @PathVariable Integer estudianteId,
      @Valid @RequestBody CompromisoPagoRequest compromiso,
      Authentication authentication) {
    return matriculas.confirmar(authentication.getName(), periodoId, estudianteId, compromiso);
  }

  @GetMapping(value = "/{matriculaId}/constancia", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> descargarConstancia(
      @PathVariable Integer matriculaId, Authentication authentication) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("constancia-matricula-" + matriculaId + ".pdf")
                .build()
                .toString())
        .body(matriculas.constancia(authentication.getName(), matriculaId));
  }
}
