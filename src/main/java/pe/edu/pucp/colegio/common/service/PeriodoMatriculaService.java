package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaRequest;
import pe.edu.pucp.colegio.common.dto.PeriodoMatriculaResponse;
import pe.edu.pucp.colegio.common.dto.TarifasMatriculaResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.EstadoPeriodoMatricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.PeriodoMatricula;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoMatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;

/** Gestiona las ventanas de tiempo habilitadas para registrar matrículas. */
@Service
public class PeriodoMatriculaService {

  private final PeriodoMatriculaRepository periodos;
  private final MatriculaRepository matriculas;
  private final PeriodoRepository periodosAcademicos;
  private final SeccionRepository secciones;
  private final Clock clock;

  public PeriodoMatriculaService(
      PeriodoMatriculaRepository periodos,
      MatriculaRepository matriculas,
      PeriodoRepository periodosAcademicos,
      SeccionRepository secciones,
      Clock clock) {
    this.periodos = periodos;
    this.matriculas = matriculas;
    this.periodosAcademicos = periodosAcademicos;
    this.secciones = secciones;
    this.clock = clock;
  }

  /**
   * Una ventana que inicia el 1 de enero corresponde a ese año; las aperturas posteriores a que
   * empiecen las clases del año corresponden al año escolar siguiente.
   */
  @Transactional(readOnly = true)
  public int anhioEscolar(LocalDate inicioVentana) {
    int anhio = inicioVentana.getYear();
    if (inicioVentana.equals(LocalDate.of(anhio, 1, 1))) {
      return anhio;
    }
    boolean clasesEmpezadas =
        periodosAcademicos
            .buscar(LocalDate.of(anhio, 1, 1), LocalDate.of(anhio, 12, 31), null, null)
            .stream()
            .anyMatch(periodo -> !periodo.getFechaInicio().isAfter(inicioVentana));
    return clasesEmpezadas ? anhio + 1 : anhio;
  }

  @Transactional
  public PeriodoMatriculaResponse crear(PeriodoMatriculaRequest request) {
    PeriodoMatriculaRequest anual = normalizarPeriodoAnual(request);
    validarAnual(anual, null);
    validarRequisitosApertura(anual);
    PeriodoMatricula periodo = new PeriodoMatricula();
    periodo.setFechaInicio(anual.fechaInicio());
    periodo.setFechaLimite(anual.fechaLimite());
    periodo.setAnhioEscolar(anual.anhioEscolar());
    periodo.setMontoMatricula(anual.montoMatricula());
    periodo.setMontoPension(anual.montoPension());
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
    PeriodoMatriculaRequest anual = normalizarPeriodoAnual(request);
    if (tieneMatriculas(periodo)) {
      throw new ConflictException(
          "No se puede modificar la apertura ni sus tarifas después de registrar matrículas");
    }
    EstadoPeriodoMatricula estado = calcularEstado(periodo);
    if (estado == EstadoPeriodoMatricula.VENCIDO && tieneMatriculas(periodo)) {
      throw new BusinessException(
          "No se puede modificar un periodo de matrícula vencido con matrículas registradas");
    }
    if (estado == EstadoPeriodoMatricula.VIGENTE) {
      if (!Objects.equals(anual.anhioEscolar(), periodo.getAnhioEscolar())) {
        throw new BusinessException("El año escolar de un periodo vigente no se puede modificar");
      }
      anual =
          new PeriodoMatriculaRequest(
              periodo.getFechaInicio(),
              fechaFinTecnica(),
              anual.anhioEscolar(),
              anual.montoMatricula(),
              anual.montoPension());
    }
    validarAnual(anual, id);
    periodo.setFechaInicio(anual.fechaInicio());
    periodo.setFechaLimite(anual.fechaLimite());
    periodo.setAnhioEscolar(anual.anhioEscolar());
    periodo.setMontoMatricula(anual.montoMatricula());
    periodo.setMontoPension(anual.montoPension());
    return respuesta(periodos.save(periodo));
  }

  @Transactional
  public void desactivar(Integer id) {
    PeriodoMatricula periodo = buscarEntidad(id);
    periodo.setActive(false);
    periodos.save(periodo);
  }

  @Transactional(readOnly = true)
  public boolean hayPeriodoVigente() {
    LocalDate hoy = LocalDate.now(clock);
    return periodos.findAllByActiveTrue().stream()
        .anyMatch(periodo -> !hoy.isBefore(periodo.getFechaInicio()));
  }

  @Transactional(readOnly = true)
  public boolean hayPeriodoVigenteParaAnhio(int anhioEscolar) {
    LocalDate hoy = LocalDate.now(clock);
    return periodos.findAllByActiveTrue().stream()
        .anyMatch(
            periodo ->
                !hoy.isBefore(periodo.getFechaInicio())
                    && Objects.equals(periodo.getAnhioEscolar(), anhioEscolar));
  }

  @Transactional(readOnly = true)
  public TarifasMatriculaResponse obtenerTarifasParaAnhio(int anhioEscolar) {
    PeriodoMatricula periodo =
        periodos
            .findFirstByAnhioEscolarOrderByIdDesc(anhioEscolar)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No hay tarifas configuradas para el año escolar " + anhioEscolar));
    return tarifas(periodo);
  }

  @Transactional(readOnly = true)
  public TarifasMatriculaResponse obtenerTarifasVigentesParaAnhio(int anhioEscolar) {
    LocalDate hoy = LocalDate.now(clock);
    PeriodoMatricula periodo =
        periodos
            .findFirstByAnhioEscolarAndActiveTrueAndFechaInicioLessThanEqualOrderByIdDesc(
                anhioEscolar, hoy)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No hay una apertura de matrícula vigente para el año escolar "
                            + anhioEscolar));
    if (periodo.getMontoMatricula() == null
        || periodo.getMontoMatricula().signum() <= 0
        || periodo.getMontoPension() == null
        || periodo.getMontoPension().signum() <= 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Configura las tarifas de matrícula y pensión antes de registrar alumnos");
    }
    return tarifas(periodo);
  }

  private TarifasMatriculaResponse tarifas(PeriodoMatricula periodo) {
    return new TarifasMatriculaResponse(periodo.getMontoMatricula(), periodo.getMontoPension());
  }

  private PeriodoMatriculaRequest normalizarPeriodoAnual(PeriodoMatriculaRequest request) {
    Integer anhio = request.anhioEscolar();
    if (anhio == null && request.fechaInicio() != null) {
      anhio = anhioEscolar(request.fechaInicio());
    }
    if (anhio == null && request.fechaLimite() != null) {
      anhio = request.fechaLimite().getYear();
    }
    if (anhio == null || anhio < 2000 || anhio > 2100) {
      throw new BusinessException("Indica un año escolar válido");
    }
    if (request.montoMatricula() == null || request.montoMatricula().signum() <= 0) {
      throw new BusinessException("Configura una tarifa de matrícula mayor que cero");
    }
    if (request.montoPension() == null || request.montoPension().signum() <= 0) {
      throw new BusinessException("Configura una pensión mensual mayor que cero");
    }
    LocalDate inicio = LocalDate.now(clock);
    return new PeriodoMatriculaRequest(
        inicio, fechaFinTecnica(), anhio, request.montoMatricula(), request.montoPension());
  }

  private void validarAnual(PeriodoMatriculaRequest request, Integer idEnEdicion) {
    int anhio = request.anhioEscolar();
    if (anhio < LocalDate.now(clock).getYear()) {
      throw new BusinessException("No se puede configurar matrícula para un año anterior");
    }
    if (periodos.existeAperturaActivaParaAnhio(anhio, idEnEdicion)) {
      throw new ConflictException("Ya existe matrícula abierta para el año escolar " + anhio);
    }
  }

  private void validarRequisitosApertura(PeriodoMatriculaRequest request) {
    int anhio = request.anhioEscolar();
    List<Periodo> anteriores =
        periodosAcademicos.buscar(
            LocalDate.of(anhio - 1, 1, 1), LocalDate.of(anhio - 1, 12, 31), null, null);
    if (anteriores.isEmpty()
        || anteriores.stream()
            .anyMatch(periodo -> !Boolean.TRUE.equals(periodo.getActasCerradas()))) {
      throw new ConflictException(
          "Cierra las actas de todas las secciones del año escolar "
              + (anhio - 1)
              + " antes de abrir matrícula");
    }

    List<Periodo> actuales =
        periodosAcademicos.buscar(
            LocalDate.of(anhio, 1, 1), LocalDate.of(anhio, 12, 31), null, null);
    List<Seccion> activas = secciones.findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc();
    if (activas.isEmpty() || actuales.isEmpty()) {
      throw new ConflictException(
          "Prepara el año académico "
              + anhio
              + " y sus grados y secciones antes de abrir matrícula");
    }
    for (Seccion seccion : activas) {
      if (seccion.getVacantesTotales() == null || seccion.getVacantesTotales() <= 0) {
        throw new ConflictException(
            "Define una capacidad mayor que cero para la sección " + seccion.getCodigo());
      }
      if (actuales.stream()
          .noneMatch(periodo -> periodo.getSeccion().getId().equals(seccion.getId()))) {
        throw new ConflictException(
            "El año académico "
                + anhio
                + " no tiene un periodo para la sección "
                + seccion.getCodigo());
      }
    }
  }

  private boolean tieneMatriculas(PeriodoMatricula periodo) {
    return matriculas.existsByActiveTrueAndPeriodoFechaInicioBetween(
        LocalDate.of(periodo.getAnhioEscolar(), 1, 1),
        LocalDate.of(periodo.getAnhioEscolar(), 12, 31));
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
    return EstadoPeriodoMatricula.VIGENTE;
  }

  private LocalDate fechaFinTecnica() {
    return LocalDate.of(2100, 12, 31);
  }

  private PeriodoMatriculaResponse respuesta(PeriodoMatricula periodo) {
    return new PeriodoMatriculaResponse(
        periodo.getId(),
        periodo.getFechaInicio(),
        periodo.getFechaLimite(),
        calcularEstado(periodo),
        tieneMatriculas(periodo),
        periodo.getAnhioEscolar(),
        periodo.getMontoMatricula(),
        periodo.getMontoPension());
  }
}
