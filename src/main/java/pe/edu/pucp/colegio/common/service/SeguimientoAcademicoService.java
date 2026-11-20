package pe.edu.pucp.colegio.common.service;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionesCalificacion;
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
  private final Clock clock;

  public SeguimientoAcademicoService(
      TareaRepository tareas,
      EntregaTareaRepository entregas,
      CalificacionRepository calificaciones,
      PeriodoRepository periodos,
      CompetenciaRepository competencias,
      MatriculaRepository matriculas,
      EvaluacionParcialRepository evaluaciones,
      EvidenciaStorageService storage,
      AuditoriaCalificacionService auditoriaCalificaciones,
      Clock clock) {
    this.tareas = tareas;
    this.entregas = entregas;
    this.calificaciones = calificaciones;
    this.periodos = periodos;
    this.competencias = competencias;
    this.matriculas = matriculas;
    this.evaluaciones = evaluaciones;
    this.storage = storage;
    this.auditoriaCalificaciones = auditoriaCalificaciones;
    this.clock = clock;
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
    asegurarEntregas(tarea);
    EntregaTarea entrega =
        entregas
            .findByTareaIdAndMatriculaIdAndActiveTrue(tareaId, matriculaId)
            .orElseThrow(
                () -> new BusinessException("La matrícula no pertenece al período de la tarea"));
    entrega.setCumplida(cumplida);
    entrega.setFechaCumplimiento(cumplida ? LocalDateTime.now(clock) : null);
    entrega.setOrigen(cumplida ? OrigenEntregaTarea.DOCENTE : null);
    entregas.save(entrega);
    return tareaResponse(tarea);
  }

  @Transactional(readOnly = true)
  public Tarea obtenerTarea(Integer tareaId) {
    return tareas
        .buscarActiva(tareaId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
  }

  @Transactional(readOnly = true)
  public TareasEstudianteService.ArchivoEntrega archivoEntrega(
      Integer tareaId, Integer matriculaId) {
    EntregaTarea entrega =
        entregas
            .findByTareaIdAndMatriculaIdAndActiveTrue(tareaId, matriculaId)
            .filter(item -> item.getArchivoClave() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La entrega no tiene un archivo adjunto"));
    return TareasEstudianteService.archivoDe(entrega, storage);
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
    Integer bimestre =
        request.bimestre() == null
            ? BimestreAcademico.actual(periodo, LocalDate.now(clock))
            : request.bimestre();
    if (evaluaciones.existsByPeriodoIdAndCompetenciaCodigoAndBimestreAndActiveTrue(
        periodo.getId(), competencia.getCodigo(), bimestre)) {
      throw new BusinessException(
          "Esta competencia ya tiene evaluaciones parciales en el bimestre; registre la nota en la evaluación correspondiente");
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
    if (archivo != null
        && !archivo.isEmpty()
        && request.evidenciaUrl() != null
        && !request.evidenciaUrl().isBlank()) {
      throw new BusinessException("Adjunte un archivo o indique una URL, no ambos");
    }
    validarUrl(request.evidenciaUrl(), archivo);
    Calificacion calificacion =
        calificaciones
            .findByPeriodoIdAndMatriculaIdAndCompetenciaCodigoAndBimestreAndConsolidadaFalse(
                periodo.getId(), matricula.getId(), competencia.getCodigo(), bimestre)
            .or(
                () ->
                    calificaciones
                        .findByPeriodoIdAndMatriculaIdAndCompetenciaCodigoAndBimestreIsNullAndConsolidadaFalse(
                            periodo.getId(), matricula.getId(), competencia.getCodigo()))
            .orElseGet(Calificacion::new);
    var calificacionAnterior = auditoriaCalificaciones.capturarFinal(calificacion);
    boolean rectificacion = calificacion.getId() != null && calificacionAnterior != null;
    boolean tieneArchivo = archivo != null && !archivo.isEmpty();
    String evidenciaUrl =
        request.evidenciaUrl() == null || request.evidenciaUrl().isBlank()
            ? null
            : request.evidenciaUrl().trim();
    String motivo = null;
    if (rectificacion) {
      boolean mismaEvidencia =
          !tieneArchivo
              && (evidenciaUrl == null || evidenciaUrl.equals(calificacion.getEvidenciaUrl()));
      if (calificacion.getNota().equals(request.nota())
          && Objects.equals(texto(calificacion.getObservacion()), texto(request.observacion()))
          && Objects.equals(calificacion.getBimestre(), bimestre)
          && mismaEvidencia) {
        throw new BusinessException("No hay cambios que guardar");
      }
      motivo = MotivoRectificacion.exigir(request.motivo());
    }
    calificacion.setPeriodo(periodo);
    calificacion.setMatricula(matricula);
    calificacion.setCompetencia(competencia);
    calificacion.setBimestre(bimestre);
    calificacion.setNota(request.nota());
    calificacion.setObservacion(request.observacion());
    calificacion.setPuntajeConsolidado(null);
    calificacion.setConsolidada(false);
    calificacion.setActive(true);
    calificacion.setFechaRegistro(LocalDateTime.now(clock));
    if (tieneArchivo) {
      EvidenciaStorageService.ArchivoGuardado guardado = storage.guardar(archivo);
      calificacion.setEvidenciaArchivo(guardado.clave());
      calificacion.setEvidenciaNombre(guardado.nombre());
      calificacion.setEvidenciaTipo(guardado.tipo());
      calificacion.setEvidenciaTamanio(guardado.tamanio());
      calificacion.setEvidenciaUrl(null);
    } else if (evidenciaUrl != null) {
      calificacion.setEvidenciaArchivo(null);
      calificacion.setEvidenciaNombre(null);
      calificacion.setEvidenciaTipo(null);
      calificacion.setEvidenciaTamanio(null);
      calificacion.setEvidenciaUrl(evidenciaUrl);
    }
    Calificacion guardada = calificaciones.save(calificacion);
    auditoriaCalificaciones.registrarFinal(
        username,
        rol,
        rectificacion
            ? AccionesCalificacion.RECTIFICAR
            : AccionesCalificacion.REGISTRAR_O_ACTUALIZAR,
        calificacionAnterior,
        guardada,
        motivo);
    long rectificaciones =
        auditoriaCalificaciones
            .rectificacionesFinales(periodo.getId(), competencia.getCodigo(), bimestre)
            .getOrDefault(AuditoriaCalificacionService.claveNota(matricula.getId(), bimestre), 0L);
    return calificacionResponse(guardada, rectificaciones);
  }

  @Transactional(readOnly = true)
  public List<HistorialCalificacionResponse> historial(Calificacion calificacion) {
    return auditoriaCalificaciones.historialFinal(calificacion).stream()
        .map(HistorialCalificacionResponse::desde)
        .toList();
  }

  private static String texto(String valor) {
    return valor == null || valor.isBlank() ? null : valor.trim();
  }

  @Transactional(readOnly = true)
  public List<CalificacionResponse> seguimientoCompetencia(
      Integer periodoId, String competenciaCodigo, Integer bimestre) {
    if (bimestre != null && (bimestre < 1 || bimestre > 4)) {
      throw new BusinessException("El bimestre debe estar entre 1 y 4");
    }
    List<Calificacion> notas =
        bimestre == null
            ? calificaciones.findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrueOrderByMatriculaId(
                periodoId, competenciaCodigo)
            : calificaciones
                .findAllByPeriodoIdAndCompetenciaCodigoAndBimestreAndActiveTrueOrderByMatriculaId(
                    periodoId, competenciaCodigo, bimestre);
    Map<String, Long> rectificaciones =
        auditoriaCalificaciones.rectificacionesFinales(periodoId, competenciaCodigo, bimestre);
    return notas.stream()
        .map(
            nota ->
                calificacionResponse(
                    nota,
                    rectificaciones.getOrDefault(
                        AuditoriaCalificacionService.claveNota(
                            nota.getMatricula().getId(), nota.getBimestre()),
                        0L)))
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
    List<EntregaTarea> registradas =
        entregas.findAllByTareaIdAndActiveTrueOrderByMatriculaId(tarea.getId());
    List<TareaResponse.EntregaResponse> estados =
        new ArrayList<>(
            registradas.stream()
                .map(
                    e ->
                        new TareaResponse.EntregaResponse(
                            e.getMatricula().getId(),
                            e.getMatricula().getEstudiante().getId(),
                            e.getMatricula().getEstudiante().getPersona().getNombreCompleto(),
                            Boolean.TRUE.equals(e.getCumplida()),
                            e.getFechaCumplimiento(),
                            e.getOrigen(),
                            e.getArchivoNombre(),
                            e.getArchivoTamanio(),
                            e.getArchivoFecha()))
                .toList());
    Set<Integer> conEntrega =
        registradas.stream().map(e -> e.getMatricula().getId()).collect(Collectors.toSet());
    // Matrículas confirmadas después de crear la tarea aparecen como pendientes sin que el GET
    // tenga que insertar filas de entrega.
    for (Matricula matricula :
        matriculas.findAllByPeriodoIdAndActiveTrueAndEstadoIn(
            tarea.getPeriodo().getId(), List.of(EstadoMatricula.CONFIRMADA))) {
      if (conEntrega.add(matricula.getId())) {
        estados.add(
            new TareaResponse.EntregaResponse(
                matricula.getId(),
                matricula.getEstudiante().getId(),
                matricula.getEstudiante().getPersona().getNombreCompleto(),
                false,
                null,
                null,
                null,
                null,
                null));
      }
    }
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

  private void asegurarEntregas(Tarea tarea) {
    List<Integer> matriculasConEntrega =
        entregas.findAllByTareaIdAndActiveTrueOrderByMatriculaId(tarea.getId()).stream()
            .map(entrega -> entrega.getMatricula().getId())
            .toList();
    for (Matricula matricula :
        matriculas.findAllByPeriodoIdAndActiveTrueAndEstadoIn(
            tarea.getPeriodo().getId(), List.of(EstadoMatricula.CONFIRMADA))) {
      if (!matriculasConEntrega.contains(matricula.getId())) {
        EntregaTarea entrega = new EntregaTarea();
        entrega.setTarea(tarea);
        entrega.setMatricula(matricula);
        entrega.setCumplida(false);
        entregas.save(entrega);
      }
    }
  }

  private CalificacionResponse calificacionResponse(Calificacion c, long rectificaciones) {
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
        c.getBimestre(),
        c.getNota(),
        c.getObservacion(),
        c.getEvidenciaArchivo() != null ? "ARCHIVO" : c.getEvidenciaUrl() != null ? "URL" : null,
        c.getEvidenciaNombre(),
        url,
        c.getEvidenciaTamanio(),
        c.getPuntajeConsolidado(),
        Boolean.TRUE.equals(c.getConsolidada()),
        c.getFechaRegistro(),
        rectificaciones);
  }
}
