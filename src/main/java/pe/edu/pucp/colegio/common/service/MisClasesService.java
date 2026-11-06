package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AlumnoClaseResponse;
import pe.edu.pucp.colegio.common.dto.CompetenciaClaseResponse;
import pe.edu.pucp.colegio.common.dto.MiClaseResponse;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;

/**
 * Clases y alumnos del docente que inicia sesión (HU 3.2). Una clase es un área dictada en una
 * sección; se obtiene de las sesiones de clase asignadas al docente.
 */
@Service
public class MisClasesService {

  private final DocenteService docenteService;
  private final SesionClaseRepository sesiones;
  private final PeriodoRepository periodos;
  private final AreaRepository areas;
  private final MatriculaRepository matriculas;
  private final Clock clock;

  public MisClasesService(
      DocenteService docenteService,
      SesionClaseRepository sesiones,
      PeriodoRepository periodos,
      AreaRepository areas,
      MatriculaRepository matriculas,
      Clock clock) {
    this.docenteService = docenteService;
    this.sesiones = sesiones;
    this.periodos = periodos;
    this.areas = areas;
    this.matriculas = matriculas;
    this.clock = clock;
  }

  @Transactional
  public List<MiClaseResponse> listar(String usuario) {
    Docente docente = docenteService.obtenerDeCuenta(usuario);
    LocalDate hoy = LocalDate.now(clock);
    List<MiClaseResponse> clases = new ArrayList<>();

    for (Object[] fila : sesiones.buscarClasesDelDocente(docente.getId())) {
      Integer seccionId = (Integer) fila[0];
      String areaCodigo = (String) fila[1];
      Periodo periodo = periodoVigente(seccionId, hoy);
      Area area = areas.findById(areaCodigo).orElse(null);
      if (periodo == null || area == null || !Boolean.TRUE.equals(area.getActivo())) {
        continue;
      }
      List<CompetenciaClaseResponse> competencias =
          area.getCompetencias().stream()
              .filter(competencia -> Boolean.TRUE.equals(competencia.getActivo()))
              .map(
                  competencia ->
                      new CompetenciaClaseResponse(
                          competencia.getCodigo(), competencia.getNombre()))
              .sorted(Comparator.comparing(CompetenciaClaseResponse::nombre))
              .toList();
      clases.add(
          new MiClaseResponse(
              periodo.getId(),
              seccionId,
              periodo.getGrado().getNumero(),
              periodo.getSeccion().getCodigo(),
              periodo.getFechaInicio().getYear(),
              area.getCodigo(),
              area.getNombre(),
              alumnosDe(periodo).size(),
              competencias));
    }

    clases.sort(
        Comparator.comparing(MiClaseResponse::areaNombre)
            .thenComparing(MiClaseResponse::gradoNumero)
            .thenComparing(MiClaseResponse::seccionCodigo));
    return clases;
  }

  @Transactional
  public List<AlumnoClaseResponse> listarAlumnos(String usuario, Integer periodoId) {
    Docente docente = docenteService.obtenerDeCuenta(usuario);
    Periodo periodo =
        periodos
            .findById(periodoId)
            .filter(encontrado -> Boolean.TRUE.equals(encontrado.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo no encontrado"));
    if (!sesiones.existsByDocenteIdAndSeccionIdAndActiveTrue(
        docente.getId(), periodo.getSeccion().getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo puedes consultar los alumnos de tus clases");
    }
    return alumnosDe(periodo).stream().map(MisClasesService::alumno).toList();
  }

  /** Periodo de la sección que incluye hoy; si no hay, el más reciente. */
  private Periodo periodoVigente(Integer seccionId, LocalDate hoy) {
    return periodos.buscarCruces(seccionId, hoy, hoy).stream()
        .findFirst()
        .orElseGet(
            () ->
                periodos
                    .findFirstBySeccionIdAndActiveTrueOrderByFechaInicioDesc(seccionId)
                    .orElse(null));
  }

  private List<Matricula> alumnosDe(Periodo periodo) {
    return matriculas
        .buscarConfirmadasEnSeccion(periodo.getSeccion().getId(), periodo.getFechaInicio())
        .stream()
        .filter(matricula -> matricula.getPeriodo().getId().equals(periodo.getId()))
        .filter(matricula -> Boolean.TRUE.equals(matricula.getEstudiante().getActive()))
        .sorted(
            Comparator.comparing(matricula -> nombreLista(matricula.getEstudiante().getPersona())))
        .toList();
  }

  private static AlumnoClaseResponse alumno(Matricula matricula) {
    Estudiante estudiante = matricula.getEstudiante();
    return new AlumnoClaseResponse(
        matricula.getId(),
        estudiante.getId(),
        estudiante.getPersona().getDni(),
        nombreLista(estudiante.getPersona()));
  }

  private static String nombreLista(Persona persona) {
    String apellidos =
        String.join(
                " ",
                Objects.toString(persona.getApellidoPaterno(), ""),
                Objects.toString(persona.getApellidoMaterno(), ""))
            .trim();
    return apellidos.isEmpty() ? persona.getNombre() : apellidos + ", " + persona.getNombre();
  }
}
