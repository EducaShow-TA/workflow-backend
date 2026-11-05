package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteResponse;
import pe.edu.pucp.colegio.common.dto.SesionClaseRequest;
import pe.edu.pucp.colegio.common.dto.SesionClaseResponse;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;

/** Gestiona sesiones de clase y la asistencia de sus estudiantes. */
@Service
public class SesionClaseService {

  private final SesionClaseRepository sesiones;
  private final AsistenciaEstudianteRepository asistencias;
  private final SeccionRepository secciones;
  private final AreaRepository areas;
  private final DocenteRepository docentes;
  private final EstudianteRepository estudiantes;
  private final VentanaMarcadoService ventanaMarcado;

  public SesionClaseService(
      SesionClaseRepository sesiones,
      AsistenciaEstudianteRepository asistencias,
      SeccionRepository secciones,
      AreaRepository areas,
      DocenteRepository docentes,
      EstudianteRepository estudiantes,
      VentanaMarcadoService ventanaMarcado) {
    this.sesiones = sesiones;
    this.asistencias = asistencias;
    this.secciones = secciones;
    this.areas = areas;
    this.docentes = docentes;
    this.estudiantes = estudiantes;
    this.ventanaMarcado = ventanaMarcado;
  }

  @Transactional
  public SesionClaseResponse crear(SesionClaseRequest request) {
    if (!request.horaFin().isAfter(request.horaInicio())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior a la hora de inicio");
    }
    Seccion seccion =
        secciones
            .findById(request.seccionId())
            .filter(seccionEncontrada -> Boolean.TRUE.equals(seccionEncontrada.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sección"));
    Area area =
        areas
            .findById(request.areaCodigo())
            .filter(areaEncontrada -> Boolean.TRUE.equals(areaEncontrada.getActivo()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el área"));
    Docente docente = buscarDocente(request.docenteId());

    if (sesiones.existsBySeccionIdAndAreaCodigoAndFechaAndHoraInicioAndActiveTrue(
        seccion.getId(), area.getCodigo(), request.fecha(), request.horaInicio())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe una sesión para la sección, área y horario indicados");
    }

    SesionClase sesion = new SesionClase();
    sesion.setSeccion(seccion);
    sesion.setArea(area);
    sesion.setDocente(docente);
    sesion.setFecha(request.fecha());
    sesion.setHoraInicio(request.horaInicio());
    sesion.setHoraFin(request.horaFin());
    try {
      return respuesta(sesiones.saveAndFlush(sesion));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe una sesión con esos datos", e);
    }
  }

  @Transactional(readOnly = true)
  public List<SesionClaseResponse> listar(LocalDate fecha, Integer seccionId) {
    return sesiones.buscar(fecha, seccionId).stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public SesionClaseResponse obtener(Integer id) {
    return respuesta(buscarSesion(id));
  }

  @Transactional
  public AsistenciaEstudianteResponse registrarAsistencia(
      Integer sesionId, AsistenciaEstudianteRequest request) {
    SesionClase sesion = buscarSesion(sesionId);
    ventanaMarcado.validar(sesion.getFecha(), sesion.getHoraInicio());
    Estudiante estudiante = buscarEstudiante(request.estudianteId());
    if (asistencias.existsBySesionClaseIdAndEstudianteIdAndActiveTrue(
        sesionId, estudiante.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La asistencia del estudiante ya fue registrada en esta sesión");
    }

    AsistenciaEstudiante asistencia = new AsistenciaEstudiante();
    asistencia.setEstudiante(estudiante);
    asistencia.setSesionClase(sesion);
    aplicarEstado(asistencia, request);
    try {
      return respuesta(asistencias.saveAndFlush(asistencia));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La asistencia ya existe para esta sesión", e);
    }
  }

  @Transactional
  public AsistenciaEstudianteResponse ajustarAsistencia(
      Integer sesionId, Integer estudianteId, AsistenciaEstudianteAjusteRequest request) {
    buscarSesion(sesionId);
    AsistenciaEstudiante asistencia =
        asistencias
            .findBySesionClaseIdAndEstudianteIdAndActiveTrue(sesionId, estudianteId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe la asistencia del estudiante en la sesión"));
    aplicarEstado(asistencia, request.estado());
    return respuesta(asistencia);
  }

  @Transactional(readOnly = true)
  public List<AsistenciaEstudianteResponse> listarAsistencias(Integer sesionId) {
    buscarSesion(sesionId);
    return asistencias.findAllBySesionClaseIdAndActiveTrueOrderByEstudianteId(sesionId).stream()
        .map(this::respuesta)
        .toList();
  }

  private void aplicarEstado(AsistenciaEstudiante asistencia, AsistenciaEstudianteRequest request) {
    aplicarEstado(asistencia, request.estado());
  }

  private void aplicarEstado(AsistenciaEstudiante asistencia, EstadoAsistencia estado) {
    switch (estado) {
      case PENDIENTE_MARCAJE -> asistencia.marcarPendiente();
      case PRESENTE -> asistencia.marcarPresente();
      case AUSENTE -> asistencia.marcarAusente();
      case TARDANZA -> asistencia.marcarTardanza();
      case JUSTIFICADA ->
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Use el endpoint de justificación y adjunte el documento de sustento");
    }
  }

  private SesionClase buscarSesion(Integer id) {
    return sesiones
        .findByIdAndActiveTrue(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la sesión de clase"));
  }

  private Docente buscarDocente(Integer id) {
    return docentes
        .findById(id)
        .filter(docente -> Boolean.TRUE.equals(docente.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el docente"));
  }

  private Estudiante buscarEstudiante(Integer id) {
    return estudiantes
        .findById(id)
        .filter(estudiante -> Boolean.TRUE.equals(estudiante.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el estudiante"));
  }

  private SesionClaseResponse respuesta(SesionClase sesion) {
    return new SesionClaseResponse(
        sesion.getId(),
        sesion.getSeccion().getId(),
        sesion.getSeccion().getCodigo(),
        sesion.getSeccion().getGrado().getNumero(),
        sesion.getArea().getCodigo(),
        sesion.getArea().getNombre(),
        sesion.getDocente().getId(),
        sesion.getDocente().getPersona().getNombreCompleto(),
        sesion.getFecha(),
        sesion.getHoraInicio(),
        sesion.getHoraFin());
  }

  private AsistenciaEstudianteResponse respuesta(AsistenciaEstudiante asistencia) {
    return new AsistenciaEstudianteResponse(
        asistencia.getId(),
        asistencia.getEstudiante().getId(),
        asistencia.getEstudiante().getPersona().getNombreCompleto(),
        asistencia.getSesionClase().getId(),
        asistencia.getEstadoAsistencia(),
        asistencia.getMotivoJustificacion(),
        asistencia.getArchivoJustificacion());
  }
}
