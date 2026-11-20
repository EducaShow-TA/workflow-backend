package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.SeccionCreateRequest;
import pe.edu.pucp.colegio.common.dto.SeccionResponse;
import pe.edu.pucp.colegio.common.dto.SeccionVacantesRequest;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;

/** Gestiona secciones y valida su capacidad frente a las matrículas activas. */
@Service
public class SeccionService {

  private final SeccionRepository secciones;
  private final GradoRepository grados;
  private final MatriculaRepository matriculas;
  private final PeriodoRepository periodos;
  private final SesionHorarioRepository sesionesHorario;

  public SeccionService(
      SeccionRepository secciones,
      GradoRepository grados,
      MatriculaRepository matriculas,
      PeriodoRepository periodos,
      SesionHorarioRepository sesionesHorario) {
    this.secciones = secciones;
    this.grados = grados;
    this.matriculas = matriculas;
    this.periodos = periodos;
    this.sesionesHorario = sesionesHorario;
  }

  @Transactional(readOnly = true)
  public List<SeccionResponse> listar() {
    return secciones.findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc().stream()
        .map(this::respuesta)
        .toList();
  }

  @Transactional
  public SeccionResponse crear(SeccionCreateRequest request) {
    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(
                request.gradoNumero(),
                request.nivelEducativo() == null
                    ? NivelEducativo.SECUNDARIA
                    : request.nivelEducativo())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe el grado indicado para el nivel educativo"));
    if (secciones.existsByGradoIdAndCodigoIgnoreCase(grado.getId(), request.nombre())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La sección ya existe para el grado indicado");
    }

    Seccion seccion = new Seccion();
    seccion.setCodigo(request.nombre());
    seccion.setVacantesTotales(request.vacantesTotales());
    grado.agregarSeccion(seccion);
    seccion = secciones.save(seccion);
    crearPeriodosEnAnhiosAbiertos(seccion);
    return respuesta(seccion);
  }

  @Transactional
  public SeccionResponse actualizarVacantes(Integer id, SeccionVacantesRequest request) {
    Seccion seccion = bloquearSeccion(id);
    long matriculados = matriculadosEnAnhioMasReciente(id);
    if (request.vacantesTotales() < matriculados) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "La capacidad no puede ser menor que los " + matriculados + " alumnos ya matriculados");
    }
    var asignacionInsuficiente =
        sesionesHorario
            .findVigentesBySeccionId(id, LocalDate.now(), LocalDate.now().getYear())
            .stream()
            .filter(sesion -> sesion.getAula().getCapacidad() < request.vacantesTotales())
            .findFirst();
    if (asignacionInsuficiente.isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "La capacidad solicitada supera la del aula "
              + asignacionInsuficiente.get().getAula().getCodigo()
              + " asignada a esta sección");
    }
    seccion.setVacantesTotales(request.vacantesTotales());
    return respuesta(seccion);
  }

  /** Las vacantes de la sección se muestran para su año escolar más reciente. */
  private long matriculadosEnAnhioMasReciente(Integer seccionId) {
    return periodos
        .findFirstBySeccionIdAndActiveTrueOrderByFechaInicioDesc(seccionId)
        .map(
            periodo ->
                matriculas.countAlumnosActivosPorSeccionEnAnhio(
                    seccionId, periodo.getFechaInicio().getYear()))
        .orElse(0L);
  }

  /** Una sección nueva se suma a los años escolares abiertos que todavía no terminan. */
  private void crearPeriodosEnAnhiosAbiertos(Seccion seccion) {
    LocalDate hoy = LocalDate.now();
    Map<Integer, List<Periodo>> porAnhio =
        periodos.findAllByActiveTrueAndFechaFinGreaterThanEqual(hoy).stream()
            .collect(
                Collectors.groupingBy(
                    periodo -> periodo.getFechaInicio().getYear(),
                    TreeMap::new,
                    Collectors.toList()));

    porAnhio
        .values()
        .forEach(
            periodosDelAnhio -> {
              LocalDate inicio =
                  periodosDelAnhio.stream()
                      .map(Periodo::getFechaInicio)
                      .min(LocalDate::compareTo)
                      .orElseThrow();
              LocalDate fin =
                  periodosDelAnhio.stream()
                      .map(Periodo::getFechaFin)
                      .max(LocalDate::compareTo)
                      .orElseThrow();
              Periodo periodo = new Periodo();
              periodo.setGrado(seccion.getGrado());
              periodo.setSeccion(seccion);
              periodo.setFechaInicio(inicio);
              periodo.setFechaFin(fin);
              periodo.setEstado(periodo.calcularEstado(hoy));
              periodos.save(periodo);
            });
  }

  private Seccion bloquearSeccion(Integer id) {
    return secciones
        .findByIdForUpdate(id)
        .filter(seccion -> Boolean.TRUE.equals(seccion.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sección no existe"));
  }

  private SeccionResponse respuesta(Seccion seccion) {
    long matriculados = matriculadosEnAnhioMasReciente(seccion.getId());
    Integer capacidad = seccion.getVacantesTotales();
    int capacidadVisible = capacidad == null || capacidad <= 0 ? 0 : capacidad;
    return new SeccionResponse(
        seccion.getId(),
        seccion.getGrado().getNumero(),
        seccion.getGrado().getNivelEducativo().name(),
        seccion.getCodigo(),
        capacidadVisible,
        matriculados,
        Math.max(0, capacidadVisible - matriculados));
  }
}
