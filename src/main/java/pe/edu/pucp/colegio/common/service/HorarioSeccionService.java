package pe.edu.pucp.colegio.common.service;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.BloqueGrillaResponse;
import pe.edu.pucp.colegio.common.dto.ConflictoSesionResponse;
import pe.edu.pucp.colegio.common.dto.HorarioSeccionCreateRequest;
import pe.edu.pucp.colegio.common.dto.HorarioSeccionResponse;
import pe.edu.pucp.colegio.common.dto.SesionHorarioRequest;
import pe.edu.pucp.colegio.common.dto.SesionHorarioResponse;
import pe.edu.pucp.colegio.common.dto.ValidacionSesionResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.BloqueHorario;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.PlantillaHorario;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.model.TipoBloqueHorario;
import pe.edu.pucp.colegio.common.model.TipoConflictoHorario;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.HorarioSeccionRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PlantillaHorarioRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Arma el horario semanal de una sección y administra las sesiones de su grilla. */
@Service
public class HorarioSeccionService {

  static final List<DayOfWeek> DIAS_LECTIVOS =
      List.of(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY);

  private static final Locale ES = Locale.forLanguageTag("es-PE");
  private static final int SIN_EXCLUIR = -1;

  private final HorarioSeccionRepository horarios;
  private final SesionHorarioRepository sesiones;
  private final SeccionRepository secciones;
  private final PlantillaHorarioRepository plantillas;
  private final AreaRepository areas;
  private final AulaRepository aulas;
  private final CuentaUsuarioRepository cuentas;
  private final MatriculaRepository matriculas;

  public HorarioSeccionService(
      HorarioSeccionRepository horarios,
      SesionHorarioRepository sesiones,
      SeccionRepository secciones,
      PlantillaHorarioRepository plantillas,
      AreaRepository areas,
      AulaRepository aulas,
      CuentaUsuarioRepository cuentas,
      MatriculaRepository matriculas) {
    this.horarios = horarios;
    this.sesiones = sesiones;
    this.secciones = secciones;
    this.plantillas = plantillas;
    this.areas = areas;
    this.aulas = aulas;
    this.cuentas = cuentas;
    this.matriculas = matriculas;
  }

  private record Resolucion(BloqueHorario bloque, Area area, CuentaUsuario docente, Aula aula) {}

  @Transactional
  public HorarioSeccionResponse crear(HorarioSeccionCreateRequest request) {
    Seccion seccion =
        secciones
            .findById(request.seccionId())
            .filter(s -> Boolean.TRUE.equals(s.getActive()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la sección indicada"));
    PlantillaHorario plantilla =
        plantillas
            .findWithBloquesById(request.plantillaId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la plantilla horaria indicada"));
    if (!Boolean.TRUE.equals(plantilla.getActive())) {
      throw new BusinessException("La plantilla horaria indicada está inactiva");
    }
    if (horarios.existsBySeccionIdAndAnio(seccion.getId(), request.anio())) {
      throw new ConflictException("La sección ya tiene un horario para el año " + request.anio());
    }

    HorarioSeccion horario = new HorarioSeccion();
    horario.setSeccion(seccion);
    horario.setPlantilla(plantilla);
    horario.setAnio(request.anio());
    return grilla(horarios.save(horario));
  }

  @Transactional(readOnly = true)
  public HorarioSeccionResponse obtener(Integer id) {
    return grilla(buscar(id));
  }

  @Transactional(readOnly = true)
  public HorarioSeccionResponse obtenerPorSeccion(Integer seccionId, Integer anio) {
    return grilla(
        horarios
            .findBySeccionIdAndAnio(seccionId, anio)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La sección no tiene un horario para el año " + anio)));
  }

  @Transactional
  public SesionHorarioResponse agregarSesion(Integer horarioId, SesionHorarioRequest request) {
    HorarioSeccion horario = buscar(horarioId);
    SesionHorario sesion = new SesionHorario();
    sesion.setHorario(horario);
    aplicar(horario, sesion, request);
    return respuesta(sesiones.save(sesion));
  }

  @Transactional
  public SesionHorarioResponse actualizarSesion(
      Integer horarioId, Integer sesionId, SesionHorarioRequest request) {
    HorarioSeccion horario = buscar(horarioId);
    SesionHorario sesion = buscarSesion(horarioId, sesionId);
    aplicar(horario, sesion, request);
    return respuesta(sesiones.save(sesion));
  }

  @Transactional
  public void eliminarSesion(Integer horarioId, Integer sesionId) {
    sesiones.delete(buscarSesion(horarioId, sesionId));
  }

  @Transactional(readOnly = true)
  public ValidacionSesionResponse validarSesion(
      Integer horarioId, SesionHorarioRequest request, Integer excluirSesionId) {
    HorarioSeccion horario = buscar(horarioId);
    if (excluirSesionId != null) {
      buscarSesion(horarioId, excluirSesionId);
    }
    Resolucion datos = resolver(horario, request);
    List<ConflictoSesionResponse> conflictos =
        detectarConflictos(horario, datos, request.dia(), excluirSesionId);
    return new ValidacionSesionResponse(conflictos.isEmpty(), conflictos);
  }

  private void aplicar(HorarioSeccion horario, SesionHorario sesion, SesionHorarioRequest request) {
    Resolucion datos = resolver(horario, request);
    List<ConflictoSesionResponse> conflictos =
        detectarConflictos(horario, datos, request.dia(), sesion.getId());
    List<ConflictoSesionResponse> cruces =
        conflictos.stream().filter(c -> c.tipo() != TipoConflictoHorario.CAPACIDAD).toList();
    if (!cruces.isEmpty()) {
      throw new ConflictException(
          cruces.stream().map(ConflictoSesionResponse::mensaje).collect(Collectors.joining(". ")));
    }
    conflictos.stream()
        .filter(c -> c.tipo() == TipoConflictoHorario.CAPACIDAD)
        .findFirst()
        .ifPresent(
            c -> {
              throw new BusinessException(c.mensaje());
            });

    sesion.setBloque(datos.bloque());
    sesion.setDia(request.dia());
    sesion.setArea(datos.area());
    sesion.setDocente(datos.docente());
    sesion.setAula(datos.aula());
  }

  private Resolucion resolver(HorarioSeccion horario, SesionHorarioRequest request) {
    BloqueHorario bloque =
        horario.getPlantilla().getBloques().stream()
            .filter(b -> b.getId().equals(request.bloqueId()))
            .findFirst()
            .orElseThrow(
                () -> new BusinessException("El bloque no pertenece a la plantilla del horario"));
    if (bloque.getTipo() != TipoBloqueHorario.PEDAGOGICO) {
      throw new BusinessException("No se pueden asignar clases en un recreo");
    }
    if (!DIAS_LECTIVOS.contains(request.dia())) {
      throw new BusinessException("El horario solo admite días de lunes a viernes");
    }
    Area area =
        areas
            .findById(request.areaCodigo())
            .filter(a -> Boolean.TRUE.equals(a.getActivo()))
            .orElseThrow(() -> new BusinessException("El área indicada no existe o está inactiva"));
    CuentaUsuario docente =
        cuentas
            .findById(request.docenteId())
            .filter(c -> c.getRol() == Rol.DOCENTE && Boolean.TRUE.equals(c.getActive()))
            .orElseThrow(
                () -> new BusinessException("El docente indicado no existe o no está activo"));
    Aula aula =
        aulas
            .findById(request.aulaId())
            .filter(a -> Boolean.TRUE.equals(a.getActive()))
            .orElseThrow(() -> new BusinessException("El aula indicada no existe o está inactiva"));
    return new Resolucion(bloque, area, docente, aula);
  }

  private List<ConflictoSesionResponse> detectarConflictos(
      HorarioSeccion horario, Resolucion datos, DayOfWeek dia, Integer sesionIdExcluida) {
    List<ConflictoSesionResponse> conflictos = new ArrayList<>();
    BloqueHorario bloque = datos.bloque();
    int excluir = sesionIdExcluida == null ? SIN_EXCLUIR : sesionIdExcluida;

    boolean ocupada =
        sesionIdExcluida == null
            ? sesiones.existsByHorarioIdAndBloqueIdAndDia(horario.getId(), bloque.getId(), dia)
            : sesiones.existsByHorarioIdAndBloqueIdAndDiaAndIdNot(
                horario.getId(), bloque.getId(), dia, sesionIdExcluida);
    if (ocupada) {
      conflictos.add(
          new ConflictoSesionResponse(
              TipoConflictoHorario.CELDA, "Ya hay una clase asignada en ese día y bloque"));
    }

    for (SesionHorario otra :
        sesiones.buscarCrucesDocente(
            datos.docente().getId(),
            dia,
            horario.getAnio(),
            bloque.getHoraInicio(),
            bloque.getHoraFin(),
            excluir)) {
      if (esMismaCelda(otra, horario, bloque)) {
        continue;
      }
      conflictos.add(
          new ConflictoSesionResponse(
              TipoConflictoHorario.DOCENTE,
              "El docente "
                  + nombreDocente(datos.docente())
                  + " ya tiene clase el "
                  + nombreDia(dia)
                  + " de "
                  + franja(otra.getBloque())
                  + " en "
                  + nombreSeccion(otra.getHorario().getSeccion())));
    }

    for (SesionHorario otra :
        sesiones.buscarCrucesAula(
            datos.aula().getId(),
            dia,
            horario.getAnio(),
            bloque.getHoraInicio(),
            bloque.getHoraFin(),
            excluir)) {
      if (esMismaCelda(otra, horario, bloque)) {
        continue;
      }
      conflictos.add(
          new ConflictoSesionResponse(
              TipoConflictoHorario.AULA,
              "El aula "
                  + datos.aula().getCodigo()
                  + " ya está ocupada el "
                  + nombreDia(dia)
                  + " de "
                  + franja(otra.getBloque())
                  + " por "
                  + nombreSeccion(otra.getHorario().getSeccion())));
    }

    Seccion seccion = horario.getSeccion();
    long alumnos =
        matriculas.countAlumnosActivosPorSeccionEnAnhio(seccion.getId(), horario.getAnio());
    if (alumnos > datos.aula().getCapacidad()) {
      conflictos.add(
          new ConflictoSesionResponse(
              TipoConflictoHorario.CAPACIDAD,
              "El aula "
                  + datos.aula().getCodigo()
                  + " tiene capacidad para "
                  + datos.aula().getCapacidad()
                  + " alumnos y "
                  + nombreSeccion(seccion)
                  + " tiene "
                  + alumnos
                  + " matriculados en "
                  + horario.getAnio()));
    }
    return conflictos;
  }

  private boolean esMismaCelda(SesionHorario otra, HorarioSeccion horario, BloqueHorario bloque) {
    return otra.getHorario().getId().equals(horario.getId())
        && otra.getBloque().getId().equals(bloque.getId());
  }

  private HorarioSeccion buscar(Integer id) {
    return horarios
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe el horario de sección indicado"));
  }

  private SesionHorario buscarSesion(Integer horarioId, Integer sesionId) {
    return sesiones
        .findByIdAndHorarioId(sesionId, horarioId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe la sesión de clase indicada"));
  }

  private HorarioSeccionResponse grilla(HorarioSeccion horario) {
    Map<Integer, List<SesionHorarioResponse>> porBloque =
        sesiones.findAllByHorarioId(horario.getId()).stream()
            .sorted(Comparator.comparing(SesionHorario::getDia))
            .collect(
                Collectors.groupingBy(
                    s -> s.getBloque().getId(),
                    Collectors.mapping(this::respuesta, Collectors.toList())));

    PlantillaHorario plantilla = horario.getPlantilla();
    List<BloqueGrillaResponse> filas =
        plantilla.getBloques().stream()
            .sorted(Comparator.comparing(BloqueHorario::getOrden))
            .map(
                b ->
                    new BloqueGrillaResponse(
                        b.getId(),
                        b.getOrden(),
                        b.getTipo(),
                        b.getHoraInicio(),
                        b.getHoraFin(),
                        porBloque.getOrDefault(b.getId(), List.of())))
            .toList();

    Seccion seccion = horario.getSeccion();
    return new HorarioSeccionResponse(
        horario.getId(),
        seccion.getId(),
        nombreSeccion(seccion),
        horario.getAnio(),
        plantilla.getId(),
        plantilla.getNombre(),
        plantilla.getTurno(),
        DIAS_LECTIVOS,
        filas);
  }

  private SesionHorarioResponse respuesta(SesionHorario sesion) {
    CuentaUsuario docente = sesion.getDocente();
    return new SesionHorarioResponse(
        sesion.getId(),
        sesion.getBloque().getId(),
        sesion.getDia(),
        sesion.getArea().getCodigo(),
        sesion.getArea().getNombre(),
        docente.getId(),
        nombreDocente(docente),
        sesion.getAula().getId(),
        sesion.getAula().getCodigo());
  }

  private String nombreDocente(CuentaUsuario docente) {
    var persona = docente.getPersona();
    if (persona == null) {
      return docente.getUsername();
    }
    return (persona.getNombre() + " " + persona.getApellidoPaterno()).trim();
  }

  private String nombreSeccion(Seccion seccion) {
    return seccion.getGrado().getNumero() + "° " + seccion.getCodigo();
  }

  private String nombreDia(DayOfWeek dia) {
    return dia.getDisplayName(TextStyle.FULL, ES);
  }

  private String franja(BloqueHorario bloque) {
    return bloque.getHoraInicio() + " a " + bloque.getHoraFin();
  }
}
