package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.CompromisoPagoRequest;
import pe.edu.pucp.colegio.common.dto.ConfirmacionMasivaMatriculaRequest;
import pe.edu.pucp.colegio.common.dto.IncidenciaConfirmacionMatriculaResponse;
import pe.edu.pucp.colegio.common.dto.MatriculaCreateRequest;
import pe.edu.pucp.colegio.common.dto.MatriculaResponse;
import pe.edu.pucp.colegio.common.exception.ValidacionConfirmacionMatriculaException;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;

/** Gestiona la matrícula, confirmación y retiro de estudiantes por periodo. */
@Service
public class MatriculaService {

  private final MatriculaRepository matriculas;
  private final EstudianteRepository estudiantes;
  private final PeriodoRepository periodos;
  private final VacantesService vacantes;
  private final SeccionRepository secciones;
  private final PeriodoMatriculaService periodosMatricula;
  private final FinanzasService finanzas;

  public MatriculaService(
      MatriculaRepository matriculas,
      EstudianteRepository estudiantes,
      PeriodoRepository periodos,
      VacantesService vacantes,
      SeccionRepository secciones,
      PeriodoMatriculaService periodosMatricula,
      FinanzasService finanzas) {
    this.matriculas = matriculas;
    this.estudiantes = estudiantes;
    this.periodos = periodos;
    this.vacantes = vacantes;
    this.secciones = secciones;
    this.periodosMatricula = periodosMatricula;
    this.finanzas = finanzas;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public MatriculaResponse crear(Integer periodoId, MatriculaCreateRequest request) {
    Periodo periodo = buscarPeriodo(periodoId);
    if (!periodosMatricula.hayPeriodoVigente()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "No hay un periodo de matrícula vigente");
    }
    secciones
        .findByIdForUpdate(periodo.getSeccion().getId())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sección"));
    Estudiante estudiante =
        estudiantes
            .findById(request.estudianteId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el estudiante indicado"));

    Matricula matricula =
        matriculas.findByPeriodoIdAndEstudianteId(periodoId, estudiante.getId()).orElse(null);
    if (matricula != null
        && Boolean.TRUE.equals(matricula.getActive())
        && matricula.getEstado() != EstadoMatricula.RETIRADA) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El estudiante ya está matriculado en este periodo");
    }

    if (vacantes.consultar(periodoId).vacantesDisponibles() == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo no tiene vacantes");
    }

    if (matricula == null) {
      matricula = new Matricula();
      matricula.setPeriodo(periodo);
      matricula.setEstudiante(estudiante);
    }
    matricula.setFechaMatricula(LocalDate.now());
    matricula.setEstado(EstadoMatricula.EN_REGISTRO);
    matricula.setActive(true);
    matricula = matriculas.save(matricula);

    return respuesta(matricula);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public List<MatriculaResponse> crearMasivamente(Integer periodoId, List<Integer> estudianteIds) {
    List<Integer> ids = estudianteIds.stream().distinct().toList();
    if (ids.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Selecciona al menos un estudiante");
    }
    List<MatriculaResponse> resultados = new ArrayList<>();
    for (Integer estudianteId : ids) {
      resultados.add(crear(periodoId, new MatriculaCreateRequest(estudianteId)));
    }
    return resultados;
  }

  @Transactional
  public MatriculaResponse confirmar(
      Integer periodoId, Integer estudianteId, CompromisoPagoRequest compromisoRequest) {
    Periodo periodo = buscarPeriodo(periodoId);
    secciones
        .findByIdForUpdate(periodo.getSeccion().getId())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sección"));
    Matricula matricula =
        matriculas
            .findByPeriodoIdAndEstudianteId(periodoId, estudianteId)
            .filter(m -> Boolean.TRUE.equals(m.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la matrícula"));
    if (matricula.getEstado() != EstadoMatricula.EN_REGISTRO) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Solo se puede confirmar una matrícula en registro");
    }
    matricula.setEstado(EstadoMatricula.CONFIRMADA);
    matricula = matriculas.save(matricula);
    finanzas.crearCompromiso(matricula, compromisoRequest);
    return respuesta(matricula);
  }

  @Transactional
  public List<MatriculaResponse> confirmarMasivamente(
      Integer periodoId, ConfirmacionMasivaMatriculaRequest request) {
    Periodo periodo = buscarPeriodo(periodoId);
    secciones
        .findByIdForUpdate(periodo.getSeccion().getId())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sección"));
    List<Integer> ids = request.estudianteIds().stream().distinct().toList();
    if (ids.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Selecciona al menos un estudiante");
    }
    finanzas.validarTerminos(request.compromiso());
    List<Matricula> pendientes = new ArrayList<>();
    List<IncidenciaConfirmacionMatriculaResponse> incidencias = new ArrayList<>();
    for (Integer estudianteId : ids) {
      Matricula matricula =
          matriculas
              .findByPeriodoIdAndEstudianteId(periodoId, estudianteId)
              .filter(m -> Boolean.TRUE.equals(m.getActive()))
              .orElse(null);
      if (matricula == null) {
        incidencias.add(
            new IncidenciaConfirmacionMatriculaResponse(
                null,
                estudianteId,
                nombreEstudiante(estudianteId),
                List.of("No existe una matrícula activa para este estudiante en el período.")));
        continue;
      }
      List<String> problemas = new ArrayList<>();
      if (matricula.getEstado() != EstadoMatricula.EN_REGISTRO) {
        problemas.add("La matrícula no está pendiente de confirmación.");
      }
      problemas.addAll(finanzas.validarRequisitosCompromiso(matricula));
      if (!problemas.isEmpty()) {
        incidencias.add(
            new IncidenciaConfirmacionMatriculaResponse(
                matricula.getId(),
                estudianteId,
                matricula.getEstudiante().getPersona().getNombreCompleto(),
                List.copyOf(problemas)));
      }
      pendientes.add(matricula);
    }
    if (!incidencias.isEmpty()) {
      throw new ValidacionConfirmacionMatriculaException(incidencias);
    }
    pendientes.forEach(m -> m.setEstado(EstadoMatricula.CONFIRMADA));
    List<Matricula> confirmadas = matriculas.saveAll(pendientes);
    CompromisoPagoRequest terminos = request.compromiso();
    confirmadas.forEach(m -> finanzas.crearCompromiso(m, terminos));
    return confirmadas.stream().map(this::respuesta).toList();
  }

  private String nombreEstudiante(Integer estudianteId) {
    return estudiantes
        .findById(estudianteId)
        .map(estudiante -> estudiante.getPersona().getNombreCompleto())
        .orElse("Estudiante #" + estudianteId);
  }

  @Transactional
  public void eliminar(Integer periodoId, Integer estudianteId) {
    buscarPeriodo(periodoId);
    Matricula matricula =
        matriculas
            .findByPeriodoIdAndEstudianteId(periodoId, estudianteId)
            .filter(m -> m.getEstado() != EstadoMatricula.RETIRADA)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El estudiante no tiene matrícula en el periodo indicado"));
    matricula.setEstado(EstadoMatricula.RETIRADA);
    finanzas.desactivarCompromisoDeMatricula(matricula.getId());
  }

  private Periodo buscarPeriodo(Integer periodoId) {
    return periodos
        .findById(periodoId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el periodo"));
  }

  private MatriculaResponse respuesta(Matricula matricula) {
    return new MatriculaResponse(
        matricula.getId(),
        matricula.getEstudiante().getId(),
        matricula.getPeriodo().getId(),
        matricula.getFechaMatricula(),
        matricula.getEstado());
  }
}
