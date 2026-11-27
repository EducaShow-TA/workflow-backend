package pe.edu.pucp.colegio.common.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.CompetenciaReporteAnualResponse;
import pe.edu.pucp.colegio.common.dto.CursoReporteAnualResponse;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse;
import pe.edu.pucp.colegio.common.dto.ReporteAnualNotasResponse;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

@Service
public class ReporteAnualNotasService {

  private final LibretaNotasService libretas;
  private final PeriodoRepository periodos;
  private final CalendarioBimestralService calendario;

  public ReporteAnualNotasService(
      LibretaNotasService libretas,
      PeriodoRepository periodos,
      CalendarioBimestralService calendario) {
    this.libretas = libretas;
    this.periodos = periodos;
    this.calendario = calendario;
  }

  @Transactional(readOnly = true)
  public ReporteAnualNotasResponse generar(
      Integer estudianteId, Integer periodoId, String username) {
    Periodo periodo =
        periodos
            .findById(periodoId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el período"));
    int ultimoBimestre = calendario.actual(periodo);
    List<LibretaNotasResponse> bimestres =
        java.util.stream.IntStream.rangeClosed(1, ultimoBimestre)
            .mapToObj(bimestre -> libretas.generar(estudianteId, periodoId, bimestre, username))
            .toList();
    LibretaNotasResponse base = bimestres.get(0);
    var periodoLibreta =
        base.periodos().stream()
            .filter(item -> item.periodoId().equals(periodoId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "El reporte anual no contiene el período solicitado"));

    Map<String, CursoBuilder> cursos = new LinkedHashMap<>();
    for (int indice = 0; indice < bimestres.size(); indice++) {
      var periodoBimestre =
          bimestres.get(indice).periodos().stream()
              .filter(item -> item.periodoId().equals(periodoId))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "El reporte anual no contiene el período solicitado"));
      for (var area : periodoBimestre.areas()) {
        CursoBuilder curso =
            cursos.computeIfAbsent(
                area.codigo(),
                ignored -> new CursoBuilder(area.codigo(), area.nombre(), ultimoBimestre));
        for (var competencia : area.competencias()) {
          CompetenciaBuilder resultado =
              curso.competencias.computeIfAbsent(
                  competencia.codigo(),
                  ignored ->
                      new CompetenciaBuilder(
                          competencia.codigo(), competencia.nombre(), curso.ultimoBimestre));
          resultado.notas.set(indice, competencia.nota());
        }
      }
    }

    List<CursoReporteAnualResponse> cursosRespuesta =
        cursos.values().stream().map(CursoBuilder::respuesta).toList();
    return new ReporteAnualNotasResponse(
        estudianteId,
        base.estudiante(),
        periodoId,
        base.anhioEscolar(),
        ultimoBimestre,
        periodoLibreta.gradoNumero(),
        periodoLibreta.seccion(),
        periodoLibreta.actasCerradas(),
        cursosRespuesta);
  }

  private static final class CursoBuilder {
    private final String codigo;
    private final String nombre;
    private final Map<String, CompetenciaBuilder> competencias = new LinkedHashMap<>();

    private CursoBuilder(String codigo, String nombre, int ultimoBimestre) {
      this.codigo = codigo;
      this.nombre = nombre;
      this.ultimoBimestre = ultimoBimestre;
    }

    private final int ultimoBimestre;

    private CursoReporteAnualResponse respuesta() {
      return new CursoReporteAnualResponse(
          codigo,
          nombre,
          competencias.values().stream().map(CompetenciaBuilder::respuesta).toList());
    }
  }

  private static final class CompetenciaBuilder {
    private final String codigo;
    private final String nombre;
    private final List<String> notas;

    private CompetenciaBuilder(String codigo, String nombre, int ultimoBimestre) {
      this.codigo = codigo;
      this.nombre = nombre;
      this.notas = new ArrayList<>(Collections.nCopies(ultimoBimestre, null));
    }

    private CompetenciaReporteAnualResponse respuesta() {
      return new CompetenciaReporteAnualResponse(
          codigo, nombre, Collections.unmodifiableList(new ArrayList<>(notas)));
    }
  }
}
