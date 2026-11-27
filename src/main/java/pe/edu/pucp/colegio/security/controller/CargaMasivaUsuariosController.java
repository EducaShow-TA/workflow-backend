package pe.edu.pucp.colegio.security.controller;

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
import pe.edu.pucp.colegio.common.service.PlantillaCargaArchivoService;
import pe.edu.pucp.colegio.common.validation.ArchivoCsv;
import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.service.CargaMasivaUsuariosService;

/** Expone la carga de usuarios desde CSV y la descarga de su plantilla. */
@RestController
@RequestMapping("/api/v1/cuentas-usuario/carga-masiva")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class CargaMasivaUsuariosController {

  private final CargaMasivaUsuariosService servicio;
  private final PlantillaCargaArchivoService plantillas;

  public CargaMasivaUsuariosController(
      CargaMasivaUsuariosService servicio, PlantillaCargaArchivoService plantillas) {
    this.servicio = servicio;
    this.plantillas = plantillas;
  }

  /** Devuelve el resumen de filas procesadas, incluso cuando algunas filas son rechazadas. */
  @PostMapping
  public ResponseEntity<CargaMasivaResultadoDTO> cargar(
      @RequestParam(value = "archivo", required = false) MultipartFile archivo,
      @RequestParam(value = "rol", required = false) String rol,
      Authentication authentication)
      throws IOException {
    CargaMasivaResultadoDTO resultado =
        servicio.cargar(
            ArchivoCsv.leerContenido(archivo), authentication.getName(), rolEsperado(rol));
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

  /** Descarga una plantilla mixta o específica de rol en CSV o Excel. */
  @GetMapping("/plantilla")
  public ResponseEntity<?> plantilla(
      @RequestParam(value = "rol", required = false) String rol,
      @RequestParam(value = "formato", defaultValue = "csv") String formato) {
    String rolNormalizado = normalizarRolPlantilla(rol);
    List<String> cabeceras = cabeceras(rolNormalizado);
    List<String> instrucciones = instrucciones(rolNormalizado);
    String nombre = rolNormalizado.equals("TODOS") ? "usuarios" : rolNormalizado.toLowerCase();
    String contexto =
        "Tipo de carga: " + (rolNormalizado.equals("TODOS") ? "mixta" : rolNormalizado);
    if ("xlsx".equalsIgnoreCase(formato)) {
      byte[] archivo =
          plantillas.generarExcel(
              "Plantilla de carga de " + nombre, contexto, cabeceras, List.of(), instrucciones);
      return ResponseEntity.ok()
          .cacheControl(CacheControl.noStore())
          .header(
              HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-" + nombre + ".xlsx")
          .contentType(
              MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(archivo);
    }
    if (!"csv".equalsIgnoreCase(formato)) {
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El formato debe ser csv o xlsx");
    }
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla-" + nombre + ".csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(plantillas.generarCsv(cabeceras, List.of()));
  }

  private static String normalizarRolPlantilla(String rol) {
    if (rol == null || rol.isBlank() || "TODOS".equalsIgnoreCase(rol.trim())) return "TODOS";
    Rol esperado = rolEsperado(rol);
    return esperado.name();
  }

  private static Rol rolEsperado(String rol) {
    if (rol == null || rol.isBlank() || "TODOS".equalsIgnoreCase(rol.trim())) return null;
    try {
      Rol esperado = Rol.valueOf(rol.trim().toUpperCase());
      if (esperado == Rol.DOCENTE || esperado == Rol.ALUMNO || esperado == Rol.APODERADO) {
        return esperado;
      }
    } catch (IllegalArgumentException ignored) {
      // Se convierte en una respuesta de entrada inválida.
    }
    throw new org.springframework.web.server.ResponseStatusException(
        HttpStatus.BAD_REQUEST, "El rol debe ser DOCENTE, ALUMNO, APODERADO o TODOS");
  }

  private static List<String> cabeceras(String rol) {
    if ("ALUMNO".equals(rol)) {
      return List.of(
          "rol",
          "dni",
          "nombres",
          "apellido_paterno",
          "apellido_materno",
          "correo",
          "fecha_nacimiento",
          "telefono",
          "dni_apoderado",
          "nombres_apoderado",
          "apellido_paterno_apoderado",
          "apellido_materno_apoderado",
          "correo_apoderado",
          "telefono_apoderado",
          "parentesco");
    }
    if ("APODERADO".equals(rol)) {
      return List.of(
          "rol",
          "dni",
          "nombres",
          "apellido_paterno",
          "apellido_materno",
          "correo",
          "telefono",
          "username",
          "dni_alumno",
          "parentesco",
          "es_principal");
    }
    if ("DOCENTE".equals(rol)) {
      return List.of(
          "rol", "dni", "nombres", "apellido_paterno", "apellido_materno", "correo", "telefono");
    }
    return List.of(
        "rol",
        "dni",
        "nombres",
        "apellido_paterno",
        "apellido_materno",
        "correo",
        "username",
        "telefono",
        "fecha_nacimiento",
        "dni_apoderado",
        "parentesco",
        "dni_alumno",
        "es_principal");
  }

  private static List<String> instrucciones(String rol) {
    if ("APODERADO".equals(rol)) {
      return List.of(
          "Completa una fila por cada vínculo entre apoderado y alumno.",
          "Puedes repetir los datos del mismo apoderado en varias filas; usa un DNI de alumno"
              + " distinto por vínculo.",
          "El DNI del alumno debe corresponder a un alumno ya registrado.",
          "Parentesco: PADRE, MADRE, TUTOR_LEGAL u OTRO. En es_principal indica SI o NO.",
          "Marca SI para un solo apoderado principal por alumno. Deja username vacío para generarlo"
              + " automáticamente.");
    }
    if ("ALUMNO".equals(rol)) {
      return List.of(
          "Completa una fila por alumno y especifica el DNI y parentesco del apoderado.",
          "Si el apoderado aún no está registrado, completa también nombres_apoderado,"
              + " apellido_paterno_apoderado, apellido_materno_apoderado y telefono_apoderado.",
          "correo_apoderado es opcional. Si el apoderado ya existe, estos datos adicionales no son"
              + " necesarios.",
          "Parentesco: PADRE, MADRE, TUTOR_LEGAL u OTRO. La cuenta del alumno se genera"
              + " automáticamente; si el apoderado es nuevo, también se genera su cuenta.",
          "Para vincular apoderados adicionales, usa la plantilla específica de apoderados.");
    }
    if ("DOCENTE".equals(rol)) {
      return List.of(
          "Completa una fila por docente. El usuario de acceso se genera automáticamente.");
    }
    return List.of(
        "Completa una fila por cuenta. Roles permitidos: DOCENTE, ALUMNO y APODERADO.",
        "Para docentes y alumnos, el usuario se genera automáticamente; no completes username.",
        "Para relacionar varios apoderados con un alumno, usa una fila por vínculo en la plantilla"
            + " de APODERADO.");
  }
}
