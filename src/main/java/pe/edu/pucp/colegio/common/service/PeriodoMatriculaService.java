package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.EstadoPeriodoMatricula;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoMatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

/** Gestiona las ventanas de tiempo habilitadas para registrar matrículas. */
@Service
public class PeriodoMatriculaService {

  private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final PeriodoMatriculaRepository periodos;
  private final MatriculaRepository matriculas;
  private final PeriodoRepository periodosAcademicos;
  private final Clock clock;

  public PeriodoMatriculaService(
      PeriodoMatriculaRepository periodos,
      MatriculaRepository matriculas,
      PeriodoRepository periodosAcademicos,
      Clock clock) {
    this.periodos = periodos;
    this.matriculas = matriculas;
    this.periodosAcademicos = periodosAcademicos;
    this.clock = clock;
  }

  /**
   * Año escolar que se matricula en una ventana: si las clases del año en que empieza ya empezaron,
   * la ventana es para el año siguiente (p. ej., noviembre de 2026 matricula para 2027).
   */
  @Transactional(readOnly = true)
  public int anhioEscolar(LocalDate inicioVentana) {
    int anhio = inicioVentana.getYear();
    boolean clasesEmpezadas =
        periodosAcademicos
            .buscar(LocalDate.of(anhio, 1, 1), LocalDate.of(anhio, 12, 31), null, null)
            .stream()
            .anyMatch(periodo -> !periodo.getFechaInicio().isAfter(inicioVentana));
    return clasesEmpezadas ? anhio + 1 : anhio;
  }

  @Transactional
  public PeriodoMatriculaResponse crear(PeriodoMatriculaRequest request) {
    validarInicioDesdeHoy(request);
    validar(request, null);
    PeriodoMatricula periodo = new PeriodoMatricula();
    periodo.setFechaInicio(request.fechaInicio());
    periodo.setFechaLimite(request.fechaLimite());
    return respuesta(periodos.save(periodo));
  }

  @Transactional(readOnly = true)
  public List<PeriodoMatriculaResponse> listar() {
    return periodos.findAllByActiveTrueOrderByFechaInicioDesc().stream()
        .map(this::respuesta)
        .toList();
  }

  @Transactional(readOnly = true)
  public PeriodoMatriculaResponse buscar(Integer id) {
    return respuesta(buscarEntidad(id));
  }

  @Transactional
  public PeriodoMatriculaResponse actualizar(Integer id, PeriodoMatriculaRequest request) {
    PeriodoMatricula periodo = buscarEntidad(id);
    EstadoPeriodoMatricula estado = calcularEstado(periodo);
    if (estado == EstadoPeriodoMatricula.VENCIDO && tieneMatriculas(periodo)) {
      throw new BusinessException(
          "No se puede modificar un periodo de matrícula vencido con matrículas registradas");
    }
    if (estado == EstadoPeriodoMatricula.VIGENTE) {
      validarEdicionVigente(periodo, request);
    } else {
      validarInicioDesdeHoy(request);
    }
    validar(request, id);
    periodo.setFechaInicio(request.fechaInicio());
    periodo.setFechaLimite(request.fechaLimite());
    return respuesta(periodos.save(periodo));
  }

  @Transactional
  public void desactivar(Integer id) {
    PeriodoMatricula periodo = buscarEntidad(id);
    if (calcularEstado(periodo) == EstadoPeriodoMatricula.VENCIDO && tieneMatriculas(periodo)) {
      throw new ConflictException(
          "El periodo de matrícula tiene matrículas registradas y se conserva como historial");
    }
    periodo.setActive(false);
    periodos.save(periodo);
  }

  @Transactional(readOnly = true)
  public boolean hayPeriodoVigente() {
    LocalDate hoy = LocalDate.now(clock);
    return periodos.existsByActiveTrueAndFechaInicioLessThanEqualAndFechaLimiteGreaterThanEqual(
        hoy, hoy);
  }

  private void validarInicioDesdeHoy(PeriodoMatriculaRequest request) {
    if (request.fechaInicio().isBefore(LocalDate.now(clock))) {
      throw new BusinessException("La fecha de inicio no puede ser anterior a hoy");
    }
  }

  /** Un periodo vigente ya abrió la matrícula: solo se puede mover su fecha límite. */
  private void validarEdicionVigente(PeriodoMatricula periodo, PeriodoMatriculaRequest request) {
    if (!request.fechaInicio().equals(periodo.getFechaInicio())) {
      throw new BusinessException("El periodo ya inició; su fecha de inicio no se puede modificar");
    }
    if (request.fechaLimite().isBefore(LocalDate.now(clock))) {
      throw new BusinessException(
          "La fecha límite no puede ser anterior a hoy ("
              + LocalDate.now(clock).format(FORMATO_FECHA)
              + ")");
    }
  }

  private void validar(PeriodoMatriculaRequest request, Integer idEnEdicion) {
    if (request.fechaInicio().getYear() < request.fechaLimite().getYear() - 1) {
      throw new BusinessException(
          "El inicio del período no puede ser más de un año calendario anterior al año escolar de la fecha límite");
    }
    if (!request.fechaLimite().isAfter(request.fechaInicio())) {
      throw new BusinessException("La fecha límite debe ser posterior a la fecha de inicio");
    }
    List<PeriodoMatricula> cruces =
        periodos.buscarCruces(request.fechaInicio(), request.fechaLimite(), idEnEdicion);
    if (!cruces.isEmpty()) {
      PeriodoMatricula cruce = cruces.get(0);
      throw new ConflictException(
          "Las fechas se cruzan con el periodo de matrícula del "
              + cruce.getFechaInicio().format(FORMATO_FECHA)
              + " al "
              + cruce.getFechaLimite().format(FORMATO_FECHA));
    }
  }

  private boolean tieneMatriculas(PeriodoMatricula periodo) {
    return matriculas.existsByActiveTrueAndFechaMatriculaBetween(
        periodo.getFechaInicio(), periodo.getFechaLimite());
  }

  private PeriodoMatricula buscarEntidad(Integer id) {
    return periodos
        .findById(id)
        .filter(p -> Boolean.TRUE.equals(p.getActive()))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe el periodo de matrícula"));
  }

  private EstadoPeriodoMatricula calcularEstado(PeriodoMatricula periodo) {
    LocalDate hoy = LocalDate.now(clock);
    if (hoy.isBefore(periodo.getFechaInicio())) {
      return EstadoPeriodoMatricula.PENDIENTE;
    }
    if (hoy.isAfter(periodo.getFechaLimite())) {
      return EstadoPeriodoMatricula.VENCIDO;
    }
    return EstadoPeriodoMatricula.VIGENTE;
  }

  private PeriodoMatriculaResponse respuesta(PeriodoMatricula periodo) {
    return new PeriodoMatriculaResponse(
        periodo.getId(),
        periodo.getFechaInicio(),
        periodo.getFechaLimite(),
        calcularEstado(periodo),
        tieneMatriculas(periodo),
        anhioEscolar(periodo.getFechaInicio()));
  }
}
