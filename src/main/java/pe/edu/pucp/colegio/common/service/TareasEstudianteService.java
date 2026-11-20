package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.TareaEstudianteResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.EntregaTarea;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.OrigenEntregaTarea;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.model.Tarea;
import pe.edu.pucp.colegio.common.repository.EntregaTareaRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.common.repository.TareaRepository;

/** Tareas del alumno autenticado y entrega de su archivo (US-3.9 y US-3.10). */
@Service
public class TareasEstudianteService {

  static final Set<String> EXTENSIONES_PERMITIDAS =
      Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png", "zip");

  private final FichaEstudianteService fichas;
  private final MatriculaRepository matriculas;
  private final TareaRepository tareas;
  private final EntregaTareaRepository entregas;
  private final SesionHorarioRepository sesiones;
  private final EvidenciaStorageService storage;
  private final Clock clock;

  public TareasEstudianteService(
      FichaEstudianteService fichas,
      MatriculaRepository matriculas,
      TareaRepository tareas,
      EntregaTareaRepository entregas,
      SesionHorarioRepository sesiones,
      EvidenciaStorageService storage,
      Clock clock) {
    this.fichas = fichas;
    this.matriculas = matriculas;
    this.tareas = tareas;
    this.entregas = entregas;
    this.sesiones = sesiones;
    this.storage = storage;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<TareaEstudianteResponse> listar(String username) {
    LocalDateTime ahora = LocalDateTime.now(clock);
    return matriculasVigentes(estudianteId(username), ahora.toLocalDate()).stream()
        .flatMap(matricula -> tareasDeMatricula(matricula, ahora).stream())
        .sorted(
            Comparator.comparing(TareaEstudianteResponse::fechaLimite)
                .thenComparing(TareaEstudianteResponse::id))
        .toList();
  }

  @Transactional(readOnly = true)
  public TareaEstudianteResponse obtener(String username, Integer tareaId) {
    Tarea tarea = tareaActiva(tareaId);
    Matricula matricula = matriculaDeTarea(estudianteId(username), tarea);
    EntregaTarea entrega =
        entregas.findByTareaIdAndMatriculaIdAndActiveTrue(tareaId, matricula.getId()).orElse(null);
    return respuesta(tarea, entrega, docentesPorArea(tarea.getPeriodo()), LocalDateTime.now(clock));
  }

  @Transactional
  public TareaEstudianteResponse entregar(String username, Integer tareaId, MultipartFile archivo) {
    Tarea tarea = tareaActiva(tareaId);
    Matricula matricula = matriculaDeTarea(estudianteId(username), tarea);
    LocalDateTime ahora = LocalDateTime.now(clock);
    if (ahora.isAfter(tarea.getFechaLimite())) {
      throw new BusinessException(
          "La fecha límite de la tarea ya pasó; ya no se puede entregar por el sistema");
    }
    if (archivo == null || archivo.isEmpty()) {
      throw new BusinessException("Adjunta el archivo de tu tarea");
    }
    if (!EXTENSIONES_PERMITIDAS.contains(extension(archivo.getOriginalFilename()))) {
      throw new BusinessException(
          "Tipo de archivo no permitido. Usa PDF, Word, Excel, PowerPoint, JPG, PNG o ZIP");
    }
    EvidenciaStorageService.ArchivoGuardado guardado = storage.guardar(archivo);
    EntregaTarea entrega =
        entregas
            .findByTareaIdAndMatriculaIdAndActiveTrue(tareaId, matricula.getId())
            .orElseGet(
                () -> {
                  EntregaTarea nueva = new EntregaTarea();
                  nueva.setTarea(tarea);
                  nueva.setMatricula(matricula);
                  return nueva;
                });
    entrega.adjuntarArchivo(
        guardado.clave(), guardado.nombre(), guardado.tipo(), guardado.tamanio(), ahora);
    entrega.setCumplida(true);
    entrega.setFechaCumplimiento(ahora);
    entrega.setOrigen(OrigenEntregaTarea.ALUMNO);
    entregas.save(entrega);
    return respuesta(tarea, entrega, docentesPorArea(tarea.getPeriodo()), ahora);
  }

  @Transactional(readOnly = true)
  public ArchivoEntrega archivo(String username, Integer tareaId) {
    Tarea tarea = tareaActiva(tareaId);
    Matricula matricula = matriculaDeTarea(estudianteId(username), tarea);
    EntregaTarea entrega =
        entregas
            .findByTareaIdAndMatriculaIdAndActiveTrue(tareaId, matricula.getId())
            .filter(item -> item.getArchivoClave() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La entrega no tiene un archivo adjunto"));
    return archivoDe(entrega);
  }

  static ArchivoEntrega archivoDe(EntregaTarea entrega, EvidenciaStorageService storage) {
    return new ArchivoEntrega(
        entrega.getArchivoNombre() == null ? "entrega" : entrega.getArchivoNombre(),
        entrega.getArchivoTipo(),
        storage.cargar(entrega.getArchivoClave()));
  }

  private ArchivoEntrega archivoDe(EntregaTarea entrega) {
    return archivoDe(entrega, storage);
  }

  private Integer estudianteId(String username) {
    return fichas.obtener(username).estudianteId();
  }

  private List<Matricula> matriculasVigentes(Integer estudianteId, LocalDate hoy) {
    List<Matricula> confirmadas =
        matriculas.findAllByEstudianteIdIn(List.of(estudianteId)).stream()
            .filter(matricula -> Boolean.TRUE.equals(matricula.getActive()))
            .filter(matricula -> matricula.getEstado() == EstadoMatricula.CONFIRMADA)
            .filter(matricula -> Boolean.TRUE.equals(matricula.getPeriodo().getActive()))
            .toList();
    List<Matricula> enCurso =
        confirmadas.stream()
            .filter(matricula -> !matricula.getPeriodo().getFechaInicio().isAfter(hoy))
            .filter(matricula -> !matricula.getPeriodo().getFechaFin().isBefore(hoy))
            .toList();
    if (!enCurso.isEmpty()) {
      return enCurso;
    }
    return confirmadas.stream()
        .filter(matricula -> !matricula.getPeriodo().getFechaInicio().isAfter(hoy))
        .max(Comparator.comparing(matricula -> matricula.getPeriodo().getFechaInicio()))
        .map(List::of)
        .orElse(List.of());
  }

  private List<TareaEstudianteResponse> tareasDeMatricula(
      Matricula matricula, LocalDateTime ahora) {
    Map<Integer, EntregaTarea> porTarea = new HashMap<>();
    for (EntregaTarea entrega : entregas.findAllByMatriculaIdAndActiveTrue(matricula.getId())) {
      porTarea.put(entrega.getTarea().getId(), entrega);
    }
    Map<String, Asignacion> docentes = docentesPorArea(matricula.getPeriodo());
    return tareas.buscarPorPeriodo(matricula.getPeriodo().getId()).stream()
        .map(tarea -> respuesta(tarea, porTarea.get(tarea.getId()), docentes, ahora))
        .toList();
  }

  private Tarea tareaActiva(Integer tareaId) {
    return tareas
        .buscarActiva(tareaId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
  }

  private Matricula matriculaDeTarea(Integer estudianteId, Tarea tarea) {
    return matriculas
        .findByPeriodoIdAndEstudianteId(tarea.getPeriodo().getId(), estudianteId)
        .filter(matricula -> Boolean.TRUE.equals(matricula.getActive()))
        .filter(matricula -> matricula.getEstado() == EstadoMatricula.CONFIRMADA)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "La tarea no corresponde a tus cursos"));
  }

  private Map<String, Asignacion> docentesPorArea(Periodo periodo) {
    Map<String, Asignacion> porCompetencia = new HashMap<>();
    for (SesionHorario sesion :
        sesiones.listarAsignacionesSeccion(
            periodo.getSeccion().getId(), periodo.getFechaInicio().getYear())) {
      Area area = sesion.getArea();
      Asignacion asignacion =
          new Asignacion(
              area.getCodigo(),
              area.getNombre(),
              sesion.getDocente().getPersona().getNombreCompleto());
      area.getCompetencias()
          .forEach(competencia -> porCompetencia.putIfAbsent(competencia.getCodigo(), asignacion));
    }
    return porCompetencia;
  }

  private TareaEstudianteResponse respuesta(
      Tarea tarea, EntregaTarea entrega, Map<String, Asignacion> docentes, LocalDateTime ahora) {
    boolean entregada = entrega != null && Boolean.TRUE.equals(entrega.getCumplida());
    boolean vencida = ahora.isAfter(tarea.getFechaLimite());
    TareaEstudianteResponse.Estado estado =
        entregada
            ? TareaEstudianteResponse.Estado.ENTREGADA
            : vencida
                ? TareaEstudianteResponse.Estado.VENCIDA
                : TareaEstudianteResponse.Estado.PENDIENTE;
    Asignacion asignacion = docentes.get(tarea.getCompetencia().getCodigo());
    Area area =
        asignacion != null
            ? null
            : tarea.getCompetencia().getAreas().stream().findFirst().orElse(null);
    return new TareaEstudianteResponse(
        tarea.getId(),
        tarea.getPeriodo().getId(),
        asignacion != null ? asignacion.areaCodigo() : area == null ? null : area.getCodigo(),
        asignacion != null ? asignacion.area() : area == null ? null : area.getNombre(),
        tarea.getCompetencia().getCodigo(),
        tarea.getCompetencia().getNombre(),
        asignacion == null ? null : asignacion.docente(),
        tarea.getTitulo(),
        tarea.getDescripcion(),
        tarea.getFechaLimite(),
        tarea.getPonderacion(),
        estado,
        entregada ? entrega.getFechaCumplimiento() : null,
        entregada ? entrega.getOrigen() : null,
        entrega == null ? null : entrega.getArchivoNombre(),
        entrega == null ? null : entrega.getArchivoTamanio(),
        !vencida);
  }

  private static String extension(String nombre) {
    if (nombre == null) return "";
    int punto = nombre.lastIndexOf('.');
    return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
  }

  private record Asignacion(String areaCodigo, String area, String docente) {}

  public record ArchivoEntrega(String nombre, String tipo, Resource recurso) {}
}
