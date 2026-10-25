package pe.edu.pucp.colegio.security.controller;

import java.io.IOException;
import java.util.Locale;
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
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;
import pe.edu.pucp.colegio.security.service.CargaMasivaUsuariosService;

/** Carga masiva de cuentas desde un CSV (US-5.9). Solo el rol ADMINISTRADOR puede usarla. */
/** Expone la carga de usuarios desde CSV y la descarga de su plantilla. */
@RestController
@RequestMapping("/api/v1/cuentas-usuario/carga-masiva")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CargaMasivaUsuariosController {

  private static final String PLANTILLA =
      """
            rol,dni,nombres,apellido_paterno,apellido_materno,correo,username,telefono,fecha_nacimiento,dni_apoderado,parentesco
            APODERADO,40111222,Rosa,Quispe,Mamani,rosa.quispe@correo.com,rosa.quispe,987654321,,,
            ALUMNO,70333444,Luis,Torres,Quispe,luis.torres@colegio.edu.pe,luis.torres,,2011-05-14,40111222,MADRE
            DOCENTE,10555666,Carlos,Vega,Rios,carlos.vega@colegio.edu.pe,carlos.vega,,,,
            """;

  private final CargaMasivaUsuariosService servicio;

  public CargaMasivaUsuariosController(CargaMasivaUsuariosService servicio) {
    this.servicio = servicio;
  }

  /** Devuelve el resumen de filas procesadas, incluso cuando algunas filas son rechazadas. */
  @PostMapping
  public ResponseEntity<CargaMasivaResultadoDTO> cargar(
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      Authentication authentication)
      throws IOException {
    if (archivo == null || archivo.isEmpty()) {
      throw new BusinessException("Debe adjuntar un archivo CSV en el campo 'archivo'");
    }
    String nombre = archivo.getOriginalFilename();
    if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new BusinessException("El archivo debe tener extensión .csv");
    }
    CargaMasivaResultadoDTO resultado =
        servicio.cargar(archivo.getBytes(), authentication.getName());
    HttpStatus estado;
    if (resultado.filasRechazadas() == resultado.totalFilas()) {
      estado = HttpStatus.UNPROCESSABLE_ENTITY;
    } else if (resultado.filasRechazadas() > 0 || resultado.usuariosCreados() == 0) {
      estado = HttpStatus.OK;
    } else {
      estado = HttpStatus.CREATED;
    }
    return ResponseEntity.status(estado).cacheControl(CacheControl.noStore()).body(resultado);
  }

  /** Descarga un CSV de ejemplo con las columnas esperadas. */
  @GetMapping("/plantilla")
  public ResponseEntity<String> plantilla() {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-usuarios.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(PLANTILLA);
  }
}
