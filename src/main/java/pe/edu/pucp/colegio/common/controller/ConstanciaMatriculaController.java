package pe.edu.pucp.colegio.common.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.colegio.common.service.ConstanciaMatriculaService;

@RestController
@RequestMapping("/api/v1/matriculas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConstanciaMatriculaController {

  private final ConstanciaMatriculaService constancias;

  public ConstanciaMatriculaController(ConstanciaMatriculaService constancias) {
    this.constancias = constancias;
  }

  @GetMapping(value = "/{matriculaId}/constancia", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> descargar(@PathVariable Integer matriculaId) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("constancia-matricula-" + matriculaId + ".pdf")
                .build()
                .toString())
        .body(constancias.generar(matriculaId));
  }
}
