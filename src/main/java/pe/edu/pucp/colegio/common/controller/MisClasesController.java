package pe.edu.pucp.colegio.common.controller;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AlumnoClaseResponse;
import pe.edu.pucp.colegio.common.dto.MiClaseResponse;
import pe.edu.pucp.colegio.common.dto.RiesgoAcademicoResponse;
import pe.edu.pucp.colegio.common.service.MisClasesService;
import pe.edu.pucp.colegio.common.service.PlantillaCargaArchivoService;

/** Clases y alumnos del docente que inicia sesión, para registrar calificaciones (HU 3.2). */
@RestController
@RequestMapping("/api/v1/docentes/mis-clases")
@PreAuthorize("hasRole('DOCENTE')")
public class MisClasesController {

  private final MisClasesService clases;
  private final PlantillaCargaArchivoService plantillas;

  public MisClasesController(MisClasesService clases, PlantillaCargaArchivoService plantillas) {
    this.clases = clases;
    this.plantillas = plantillas;
  }

  @GetMapping
  public List<MiClaseResponse> listar(Authentication authentication) {
    return clases.listar(authentication.getName());
  }

  @GetMapping("/{periodoId}/alumnos")
  public List<AlumnoClaseResponse> listarAlumnos(
      @PathVariable Integer periodoId, Authentication authentication) {
    return clases.listarAlumnos(authentication.getName(), periodoId);
  }

  @GetMapping("/{periodoId}/plantilla-notas")
  public ResponseEntity<?> plantillaNotas(
      @PathVariable Integer periodoId,
      @RequestParam String competenciaCodigo,
      @RequestParam Integer bimestre,
      @RequestParam(defaultValue = "xlsx") String formato,
      Authentication authentication) {
    if (bimestre < 1 || bimestre > 4) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El bimestre debe estar entre 1 y 4");
    }
    MiClaseResponse clase =
        clases.listar(authentication.getName()).stream()
            .filter(
                item ->
                    item.periodoId().equals(periodoId)
                        && item.competencias().stream()
                            .anyMatch(
                                competencia -> competencia.codigo().equals(competenciaCodigo)))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Solo puedes descargar plantillas de tus clases"));
    List<AlumnoClaseResponse> alumnos = clases.listarAlumnos(authentication.getName(), periodoId);
    List<String> cabeceras = List.of("dni", "nombre", "nota", "observacion");
    List<List<String>> filas =
        alumnos.stream()
            .map(alumno -> List.of(alumno.dni(), alumno.nombreCompleto(), "", ""))
            .toList();
    String contexto =
        "Año "
            + clase.anhio()
            + " · "
            + clase.areaNombre()
            + " · "
            + clase.gradoNumero()
            + "° "
            + clase.seccionCodigo()
            + " · "
            + competenciaCodigo
            + " · Bimestre "
            + bimestre;
    String nombre =
        "plantilla-notas-"
            + clase.gradoNumero()
            + "-"
            + clase.seccionCodigo()
            + "-bimestre-"
            + bimestre;
    List<String> instrucciones =
        List.of(
            "La lista corresponde a la clase, competencia y bimestre seleccionados.",
            "No modifiques la columna dni; completa nota con AD, A, B o C.",
            "La columna nombre es solo de referencia. observacion es opcional.");
    if ("csv".equalsIgnoreCase(formato)) {
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombre + ".csv")
          .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
          .body(plantillas.generarCsv(cabeceras, filas));
    }
    if (!"xlsx".equalsIgnoreCase(formato)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato debe ser csv o xlsx");
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombre + ".xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(
            plantillas.generarExcel(
                "Plantilla de notas", contexto, cabeceras, filas, instrucciones));
  }

  @GetMapping("/{periodoId}/riesgo-academico")
  public RiesgoAcademicoResponse riesgoAcademico(
      @PathVariable Integer periodoId,
      @RequestParam String areaCodigo,
      Authentication authentication) {
    return clases.riesgoAcademico(authentication.getName(), periodoId, areaCodigo);
  }
}
