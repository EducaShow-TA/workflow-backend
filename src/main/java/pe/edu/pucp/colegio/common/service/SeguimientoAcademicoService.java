package pe.edu.pucp.colegio.common.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.colegio.common.dto.*;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.*;
import pe.edu.pucp.colegio.common.repository.*;

@Service
public class SeguimientoAcademicoService {
  private final TareaRepository tareas;
  private final EntregaTareaRepository entregas;
  private final CalificacionRepository calificaciones;
  private final EvaluacionParcialRepository evaluaciones;
  private final PeriodoRepository periodos;
  private final CompetenciaRepository competencias;
  private final MatriculaRepository matriculas;
  private final EvidenciaStorageService storage;
  private final AuditoriaCalificacionService auditoriaCalificaciones;

  public SeguimientoAcademicoService(
      TareaRepository tareas,
      EntregaTareaRepository entregas,
      CalificacionRepository calificaciones,
      PeriodoRepository periodos,
      CompetenciaRepository competencias,
      MatriculaRepository matriculas,
      EvaluacionParcialRepository evaluaciones,
      EvidenciaStorageService storage,
      AuditoriaCalificacionService auditoriaCalificaciones) {
    this.tareas = tareas;
    this.entregas = entregas;
    this.calificaciones = calificaciones;
    this.periodos = periodos;
    this.competencias = competencias;
    this.matriculas = matriculas;
    this.evaluaciones = evaluaciones;
    this.storage = storage;
    this.auditoriaCalificaciones = auditoriaCalificaciones;
  }

  @Transactional
  public TareaResponse crearTarea(TareaRequest request) {
    Periodo periodo = periodo(request.periodoId());
    Competencia competencia = competencia(request.competenciaCodigo());
    Tarea tarea = new Tarea();
    tarea.setPeriodo(periodo);
    tarea.setCompetencia(competencia);
    tarea.setTitulo(request.titulo().trim());
    tarea.setDescripcion(request.descripcion().trim());
    tarea.setFechaLimite(request.fechaLimite());
    tarea.setPonderacion(request.ponderacion());
    tarea = tareas.save(tarea);
    List<Matricula> roster =
        matriculas.findAllByPeriodoIdAndActiveTrueAndEstadoIn(
            periodo.getId(), List.of(EstadoMatricula.CONFIRMADA));
    for (Matricula matricula : roster) {
      EntregaTarea entrega = new EntregaTarea();
      entrega.setTarea(tarea);
      entrega.setMatricula(matricula);
      entrega.setCumplida(false);
      entregas.save(entrega);
    }
    return tareaResponse(tarea);
  }

  @Transactional(readOnly = true)
  public List<TareaResponse> listarTareas(Integer periodoId) {
    return tareas.buscarPorPeriodo(periodoId).stream().map(this::tareaResponse).toList();
  }

  @Transactional
  public TareaResponse marcarEntrega(Integer tareaId, Integer matriculaId, boolean cumplida) {
    Tarea tarea =
        tareas
            .buscarActiva(tareaId)
            .orElseThrow(() -> new BusinessException("Tarea no encontrada"));
    EntregaTarea entrega =
        entregas
            .findByTareaIdAndMatriculaIdAndActiveTrue(tareaId, matriculaId)
            .orElseThrow(
                () -> new BusinessException("La matrícula no pertenece al período de la tarea"));
    entrega.setCumplida(cumplida);
    entrega.setFechaCumplimiento(cumplida ? LocalDateTime.now() : null);
    entregas.save(entrega);
    return tareaResponse(tarea);
  }

  @Transactional
  public CalificacionResponse registrarCalificacion(
      CalificacionRequest request, MultipartFile archivo, String username, String rol) {
    Periodo periodo =
        periodos
            .findByIdAndActiveTrueForUpdate(request.periodoId())
            .orElseThrow(() -> new BusinessException("Período no encontrado"));
    if (Boolean.TRUE.equals(periodo.getActasCerradas())) {
      throw new BusinessException(
          "No se pueden modificar calificaciones: las actas están cerradas");
    }
    Competencia competencia = competencia(request.competenciaCodigo());
    if (evaluaciones.existsByPeriodoIdAndCompetenciaCodigoAndActiveTrue(
        periodo.getId(), competencia.getCodigo())) {
      throw new BusinessException(
          "Esta competencia ya tiene evaluaciones parciales; registre la nota en la evaluación correspondiente");
    }
    Matricula matricula =
        matriculas
            .findById(request.matriculaId())
            .filter(Matricula::getActive)
            .orElseThrow(() -> new BusinessException("Matrícula no encontrada"));
    if (!matricula.getPeriodo().getId().equals(periodo.getId())
        || matricula.getEstado() != EstadoMatricula.CONFIRMADA) {
      throw new BusinessException("La matrícula no está confirmada en el período indicado");
    }
    if ((archivo == null || archivo.isEmpty())
        == (request.evidenciaUrl() == null || request.evidenciaUrl().isBlank())) {
      throw new BusinessException("Adjunte un archivo o indique una URL de evidencia");
    }
    validarUrl(request.evidenciaUrl(), archivo);
    Calificacion calificacion =
        calificaciones
            .findByPeriodoIdAndMatriculaIdAndCompetenciaCodigo(
                periodo.getId(), matricula.getId(), competencia.getCodigo())
            .orElseGet(Calificacion::new);
    var calificacionAnterior = auditoriaCalificaciones.capturarFinal(calificacion);
    calificacion.setPeriodo(periodo);
    calificacion.setMatricula(matricula);
    calificacion.setCompetencia(competencia);
    calificacion.setNota(request.nota());
    calificacion.setObservacion(request.observacion());
    calificacion.setPuntajeConsolidado(null);
    calificacion.setConsolidada(false);
    calificacion.setActive(true);
    calificacion.setFechaRegistro(LocalDateTime.now());
    if (archivo != null && !archivo.isEmpty()) {
      EvidenciaStorageService.ArchivoGuardado guardado = storage.guardar(archivo);
      calificacion.setEvidenciaArchivo(guardado.clave());
      calificacion.setEvidenciaNombre(guardado.nombre());
      calificacion.setEvidenciaTipo(guardado.tipo());
      calificacion.setEvidenciaTamanio(guardado.tamanio());
      calificacion.setEvidenciaUrl(null);
    } else {
      calificacion.setEvidenciaArchivo(null);
      calificacion.setEvidenciaNombre(null);
      calificacion.setEvidenciaTipo(null);
      calificacion.setEvidenciaTamanio(null);
      calificacion.setEvidenciaUrl(request.evidenciaUrl().trim());
    }
    Calificacion guardada = calificaciones.save(calificacion);
    auditoriaCalificaciones.registrarFinal(
        username, rol, "REGISTRAR_O_ACTUALIZAR", calificacionAnterior, guardada);
    return calificacionResponse(guardada);
  }

  @Transactional(readOnly = true)
  public List<CalificacionResponse> seguimientoCompetencia(
      Integer periodoId, String competenciaCodigo) {
    return calificaciones
        .findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrueOrderByMatriculaId(
            periodoId, competenciaCodigo)
        .stream()
        .map(this::calificacionResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Calificacion obtenerCalificacion(Integer id) {
    return calificaciones
        .findByIdAndActiveTrue(id)
        .orElseThrow(() -> new BusinessException("Calificación no encontrada"));
  }

  private Periodo periodo(Integer id) {
    return periodos
        .findById(id)
        .filter(Periodo::getActive)
        .orElseThrow(() -> new BusinessException("Período no encontrado"));
  }

  private Competencia competencia(String codigo) {
    Competencia c =
        competencias
            .findById(codigo)
            .orElseThrow(() -> new BusinessException("Competencia no encontrada"));
    if (!Boolean.TRUE.equals(c.getActivo()))
      throw new BusinessException("La competencia está inactiva");
    return c;
  }

  private void validarUrl(String url, MultipartFile archivo) {
    if (archivo != null && !archivo.isEmpty() || url == null || url.isBlank()) return;
    try {
      URI uri = URI.create(url.trim());
      if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          || uri.getHost() == null) throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) {
      throw new BusinessException("La URL de evidencia debe usar HTTP o HTTPS");
    }
  }

  private TareaResponse tareaResponse(Tarea tarea) {
    List<TareaResponse.EntregaResponse> estados =
        entregas.findAllByTareaIdAndActiveTrueOrderByMatriculaId(tarea.getId()).stream()
            .map(
                e ->
                    new TareaResponse.EntregaResponse(
                        e.getMatricula().getId(),
                        e.getMatricula().getEstudiante().getId(),
                        e.getMatricula().getEstudiante().getPersona().getNombreCompleto(),
                        Boolean.TRUE.equals(e.getCumplida()),
                        e.getFechaCumplimiento()))
            .toList();
    return new TareaResponse(
        tarea.getId(),
        tarea.getPeriodo().getId(),
        tarea.getCompetencia().getCodigo(),
        tarea.getCompetencia().getNombre(),
        tarea.getTitulo(),
        tarea.getDescripcion(),
        tarea.getFechaLimite(),
        tarea.getPonderacion(),
        estados);
  }

  private CalificacionResponse calificacionResponse(Calificacion c) {
    String url =
        c.getEvidenciaArchivo() == null
            ? c.getEvidenciaUrl()
            : "/api/v1/evidencias/" + c.getId() + "/archivo";
    return new CalificacionResponse(
        c.getId(),
        c.getMatricula().getId(),
        c.getMatricula().getEstudiante().getId(),
        c.getMatricula().getEstudiante().getPersona().getNombreCompleto(),
        c.getCompetencia().getCodigo(),
        c.getNota(),
        c.getObservacion(),
        c.getEvidenciaArchivo() != null ? "ARCHIVO" : c.getEvidenciaUrl() != null ? "URL" : null,
        c.getEvidenciaNombre(),
        url,
        c.getEvidenciaTamanio(),
        c.getPuntajeConsolidado(),
        Boolean.TRUE.equals(c.getConsolidada()),
        c.getFechaRegistro());
  }
}
