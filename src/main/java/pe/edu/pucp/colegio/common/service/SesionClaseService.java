package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteAjusteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteRequest;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteResponse;
import pe.edu.pucp.colegio.common.dto.FilaListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.ListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.RegistroAsistenciaSesionRequest;
import pe.edu.pucp.colegio.common.dto.SesionClaseRequest;
import pe.edu.pucp.colegio.common.dto.SesionClaseResponse;
import pe.edu.pucp.colegio.common.dto.SesionClaseResumenResponse;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Gestiona sesiones de clase y la asistencia de sus estudiantes. */
@Service
public class SesionClaseService {

  /** Quién hace la operación: el docente solo puede tocar sus propias sesiones. */
  public record Acceso(String usuario, boolean administrador) {}

  private final SesionClaseRepository sesiones;
  private final AsistenciaEstudianteRepository asistencias;
  private final SeccionRepository secciones;
  private final AreaRepository areas;
  private final DocenteRepository docentes;
  private final EstudianteRepository estudiantes;
  private final VentanaMarcadoService ventanaMarcado;
  private final MatriculaRepository matriculas;
  private final DocenteService docenteService;
  private final CuentaUsuarioRepository cuentas;
  private final Clock clock;

  public SesionClaseService(
      SesionClaseRepository sesiones,
      AsistenciaEstudianteRepository asistencias,
      SeccionRepository secciones,
      AreaRepository areas,
      DocenteRepository docentes,
      EstudianteRepository estudiantes,
      VentanaMarcadoService ventanaMarcado,
      MatriculaRepository matriculas,
      DocenteService docenteService,
      CuentaUsuarioRepository cuentas,
      Clock clock) {
    this.sesiones = sesiones;
    this.asistencias = asistencias;
    this.secciones = secciones;
    this.areas = areas;
    this.docentes = docentes;
    this.estudiantes = estudiantes;
    this.ventanaMarcado = ventanaMarcado;
    this.matriculas = matriculas;
    this.docenteService = docenteService;
    this.cuentas = cuentas;
    this.clock = clock;
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
  public List<SesionClaseResumenResponse> listar(LocalDate fecha, Integer seccionId) {
    return sesiones.buscar(fecha, seccionId).stream().map(this::resumen).toList();
  }

  @Transactional
  public List<SesionClaseResumenResponse> listarDelDocente(String usuario, LocalDate fecha) {
    Docente docente = docenteService.obtenerDeCuenta(usuario);
    LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
    return sesiones
        .findAllByDocenteIdAndFechaAndActiveTrueAndCanceladaFalseOrderByHoraInicioAsc(
            docente.getId(), dia)
        .stream()
        .map(this::resumen)
        .toList();
  }

  @Transactional
  public ListaAsistenciaResponse obtenerLista(Integer sesionId, Acceso acceso) {
    SesionClase sesion = buscarSesion(sesionId);
    exigirAcceso(sesion, acceso);
    return lista(sesion);
  }

  /**
   * Guarda la asistencia de varios alumnos a la vez. Registrar por primera vez exige la ventana de
   * marcado (salvo al administrador); corregir lo ya registrado se permite en cualquier momento.
   */
  @Transactional
  public ListaAsistenciaResponse guardarAsistencias(
      Integer sesionId, RegistroAsistenciaSesionRequest request, Acceso acceso) {
    SesionClase sesion = buscarSesion(sesionId);
    exigirAcceso(sesion, acceso);

    Map<Integer, Estudiante> matriculados = new HashMap<>();
    alumnosMatriculados(sesion).forEach(e -> matriculados.put(e.getId(), e));
    Map<Integer, AsistenciaEstudiante> existentes = new HashMap<>();
    asistencias
        .findAllBySesionClaseIdAndActiveTrueOrderByEstudianteId(sesionId)
        .forEach(a -> existentes.put(a.getEstudiante().getId(), a));

    Set<Integer> vistos = new HashSet<>();
    boolean hayNuevos = false;
    for (AsistenciaEstudianteRequest fila : request.asistencias()) {
      if (!vistos.add(fila.estudianteId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Un alumno aparece más de una vez en la lista");
      }
      if (fila.estado() == EstadoAsistencia.PENDIENTE_MARCAJE
          || fila.estado() == EstadoAsistencia.JUSTIFICADA) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Cada alumno debe quedar como presente, tardanza o ausente");
      }
      AsistenciaEstudiante existente = existentes.get(fila.estudianteId());
      if (existente == null && !matriculados.containsKey(fila.estudianteId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Hay alumnos que no están matriculados en esta sección");
      }
      if (existente != null
          && existente.getEstadoAsistencia() == EstadoAsistencia.JUSTIFICADA
          && fila.estado() != EstadoAsistencia.JUSTIFICADA) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "La asistencia de "
                + nombreLista(existente.getEstudiante().getPersona())
                + " está justificada y no se puede cambiar aquí");
      }
      hayNuevos |=
          existente == null
              || existente.getEstadoAsistencia() == EstadoAsistencia.PENDIENTE_MARCAJE;
    }
    if (hayNuevos && !acceso.administrador()) {
      ventanaMarcado.validar(sesion.getFecha(), sesion.getHoraInicio());
    }

    LocalDateTime ahora = LocalDateTime.now(clock);
    for (AsistenciaEstudianteRequest fila : request.asistencias()) {
      AsistenciaEstudiante asistencia = existentes.get(fila.estudianteId());
      if (asistencia == null) {
        asistencia = new AsistenciaEstudiante();
        asistencia.setEstudiante(matriculados.get(fila.estudianteId()));
        asistencia.setSesionClase(sesion);
        aplicarEstado(asistencia, fila.estado());
        asistencia.anotarRegistro(acceso.usuario(), ahora);
        asistencias.save(asistencia);
      } else if (asistencia.getEstadoAsistencia() == EstadoAsistencia.PENDIENTE_MARCAJE) {
        aplicarEstado(asistencia, fila.estado());
        asistencia.anotarRegistro(acceso.usuario(), ahora);
      } else if (asistencia.getEstadoAsistencia() != fila.estado()) {
        aplicarEstado(asistencia, fila.estado());
        asistencia.anotarCorreccion(acceso.usuario(), ahora);
      }
    }
    asistencias.flush();
    return lista(sesion);
  }

  @Transactional(readOnly = true)
  public SesionClaseResponse obtener(Integer id) {
    return respuesta(buscarSesion(id));
  }

  @Transactional
  public AsistenciaEstudianteResponse registrarAsistencia(
      Integer sesionId, AsistenciaEstudianteRequest request, Acceso acceso) {
    SesionClase sesion = buscarSesion(sesionId);
    exigirAcceso(sesion, acceso);
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
    asistencia.anotarRegistro(acceso.usuario(), LocalDateTime.now(clock));
    try {
      return respuesta(asistencias.saveAndFlush(asistencia));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La asistencia ya existe para esta sesión", e);
    }
  }

  @Transactional
  public AsistenciaEstudianteResponse ajustarAsistencia(
      Integer sesionId,
      Integer estudianteId,
      AsistenciaEstudianteAjusteRequest request,
      Acceso acceso) {
    exigirAcceso(buscarSesion(sesionId), acceso);
    AsistenciaEstudiante asistencia =
        asistencias
            .findBySesionClaseIdAndEstudianteIdAndActiveTrue(sesionId, estudianteId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe la asistencia del estudiante en la sesión"));
    if (asistencia.getEstadoAsistencia() != request.estado()) {
      aplicarEstado(asistencia, request.estado());
      asistencia.anotarCorreccion(acceso.usuario(), LocalDateTime.now(clock));
    }
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

  private void exigirAcceso(SesionClase sesion, Acceso acceso) {
    if (acceso.administrador()) {
      return;
    }
    Docente docente = docenteService.obtenerDeCuenta(acceso.usuario());
    if (!docente.getId().equals(sesion.getDocente().getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo el docente de la sesión puede gestionar su asistencia");
    }
  }

  /** Alumnos con matrícula confirmada en la sección durante el periodo de la sesión. */
  private List<Estudiante> alumnosMatriculados(SesionClase sesion) {
    return matriculas
        .buscarConfirmadasEnSeccion(sesion.getSeccion().getId(), sesion.getFecha())
        .stream()
        .map(Matricula::getEstudiante)
        .filter(estudiante -> Boolean.TRUE.equals(estudiante.getActive()))
        .toList();
  }

  private SesionClaseResumenResponse resumen(SesionClase sesion) {
    return resumen(
        sesion,
        alumnosMatriculados(sesion).size(),
        asistencias.findAllBySesionClaseIdAndActiveTrueOrderByEstudianteId(sesion.getId()));
  }

  private SesionClaseResumenResponse resumen(
      SesionClase sesion, int matriculados, List<AsistenciaEstudiante> registradas) {
    Map<EstadoAsistencia, Integer> conteo = new HashMap<>();
    registradas.forEach(a -> conteo.merge(a.getEstadoAsistencia(), 1, Integer::sum));
    int registrados =
        (int)
            registradas.stream()
                .filter(a -> a.getEstadoAsistencia() != EstadoAsistencia.PENDIENTE_MARCAJE)
                .count();
    LocalDateTime registradaEn =
        registradas.stream()
            .map(AsistenciaEstudiante::getRegistradaEn)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    return new SesionClaseResumenResponse(
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
        sesion.getHoraFin(),
        Math.max(matriculados, registradas.size()),
        registrados,
        conteo.getOrDefault(EstadoAsistencia.PRESENTE, 0),
        conteo.getOrDefault(EstadoAsistencia.TARDANZA, 0),
        conteo.getOrDefault(EstadoAsistencia.AUSENTE, 0),
        conteo.getOrDefault(EstadoAsistencia.JUSTIFICADA, 0),
        ventanaMarcado.apertura(sesion.getFecha(), sesion.getHoraInicio()),
        ventanaMarcado.cierre(sesion.getFecha(), sesion.getHoraInicio()),
        ventanaMarcado.estaAbierta(sesion.getFecha(), sesion.getHoraInicio()),
        registradaEn);
  }

  private ListaAsistenciaResponse lista(SesionClase sesion) {
    List<Estudiante> matriculados = alumnosMatriculados(sesion);
    List<AsistenciaEstudiante> registradas =
        asistencias.findAllBySesionClaseIdAndActiveTrueOrderByEstudianteId(sesion.getId());

    Map<Integer, AsistenciaEstudiante> porAlumno = new HashMap<>();
    registradas.forEach(a -> porAlumno.put(a.getEstudiante().getId(), a));
    Map<Integer, Estudiante> alumnos = new LinkedHashMap<>();
    matriculados.forEach(e -> alumnos.put(e.getId(), e));
    registradas.forEach(a -> alumnos.putIfAbsent(a.getEstudiante().getId(), a.getEstudiante()));

    Set<Integer> idsMatriculados = new HashSet<>(alumnos.size());
    matriculados.forEach(e -> idsMatriculados.add(e.getId()));
    List<FilaListaAsistenciaResponse> filas = new ArrayList<>();
    for (Estudiante estudiante : alumnos.values()) {
      AsistenciaEstudiante asistencia = porAlumno.get(estudiante.getId());
      EstadoAsistencia estado = asistencia == null ? null : asistencia.getEstadoAsistencia();
      filas.add(
          new FilaListaAsistenciaResponse(
              estudiante.getId(),
              estudiante.getPersona().getDni(),
              nombreLista(estudiante.getPersona()),
              asistencia == null ? null : asistencia.getId(),
              estado == EstadoAsistencia.PENDIENTE_MARCAJE ? null : estado,
              idsMatriculados.contains(estudiante.getId())));
    }
    filas.sort(
        Comparator.comparing(
            FilaListaAsistenciaResponse::nombreCompleto, String.CASE_INSENSITIVE_ORDER));

    AsistenciaEstudiante primerRegistro =
        registradas.stream()
            .filter(a -> a.getRegistradaEn() != null)
            .min(Comparator.comparing(AsistenciaEstudiante::getRegistradaEn))
            .orElse(null);
    AsistenciaEstudiante ultimaCorreccion =
        registradas.stream()
            .filter(a -> a.getCorregidaEn() != null)
            .max(Comparator.comparing(AsistenciaEstudiante::getCorregidaEn))
            .orElse(null);
    return new ListaAsistenciaResponse(
        resumen(sesion, matriculados.size(), registradas),
        primerRegistro == null ? null : nombreUsuario(primerRegistro.getRegistradaPor()),
        ultimaCorreccion == null ? null : ultimaCorreccion.getCorregidaEn(),
        ultimaCorreccion == null ? null : nombreUsuario(ultimaCorreccion.getCorregidaPor()),
        filas);
  }

  /** "Apellidos, Nombres", como se ordenan las listas del aula. */
  private static String nombreLista(Persona persona) {
    String apellidos =
        String.join(
                " ",
                Objects.toString(persona.getApellidoPaterno(), ""),
                Objects.toString(persona.getApellidoMaterno(), ""))
            .trim();
    return apellidos.isEmpty() ? persona.getNombre() : apellidos + ", " + persona.getNombre();
  }

  private String nombreUsuario(String usuario) {
    if (usuario == null) {
      return null;
    }
    return cuentas
        .findByUsername(usuario)
        .map(
            cuenta ->
                cuenta.getPersona() == null ? usuario : cuenta.getPersona().getNombreCompleto())
        .orElse(usuario);
  }

  private SesionClase buscarSesion(Integer id) {
    SesionClase sesion =
        sesiones
            .findByIdAndActiveTrue(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la sesión de clase"));
    if (Boolean.TRUE.equals(sesion.getCancelada())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La sesión fue cancelada");
    }
    return sesion;
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
