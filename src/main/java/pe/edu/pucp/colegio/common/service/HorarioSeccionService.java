package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.PlantillaHorario;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.model.TipoBloqueHorario;
import pe.edu.pucp.colegio.common.model.TipoConflictoHorario;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.HorarioSeccionRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PlantillaHorarioRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
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
  private final SesionClaseRepository sesionesClase;
  private final AsistenciaEstudianteRepository asistencias;
  private final DocenteRepository docentes;
  private final SeccionRepository secciones;
  private final PlantillaHorarioRepository plantillas;
  private final AreaRepository areas;
  private final AulaRepository aulas;
  private final CuentaUsuarioRepository cuentas;
  private final MatriculaRepository matriculas;
  private final GeneracionSesionClaseService generacionSesiones;
  private final Clock clock;
  private final AuditoriaHorarioService auditoria;

  public HorarioSeccionService(
      HorarioSeccionRepository horarios,
      SesionHorarioRepository sesiones,
      SesionClaseRepository sesionesClase,
      AsistenciaEstudianteRepository asistencias,
      DocenteRepository docentes,
      SeccionRepository secciones,
      PlantillaHorarioRepository plantillas,
      AreaRepository areas,
      AulaRepository aulas,
      CuentaUsuarioRepository cuentas,
      MatriculaRepository matriculas,
      GeneracionSesionClaseService generacionSesiones,
      Clock clock,
      AuditoriaHorarioService auditoria) {
    this.horarios = horarios;
    this.sesiones = sesiones;
    this.sesionesClase = sesionesClase;
    this.asistencias = asistencias;
    this.docentes = docentes;
    this.secciones = secciones;
    this.plantillas = plantillas;
    this.areas = areas;
    this.aulas = aulas;
    this.cuentas = cuentas;
    this.matriculas = matriculas;
    this.generacionSesiones = generacionSesiones;
    this.clock = clock;
    this.auditoria = auditoria;
  }

  private record Resolucion(BloqueHorario bloque, Area area, Docente docente, Aula aula) {}

  public record DisponibilidadDocente(
      DayOfWeek dia,
      java.time.LocalTime horaInicio,
      java.time.LocalTime horaFin,
      String seccion,
      String area) {}

  @Transactional(readOnly = true)
  public List<DisponibilidadDocente> disponibilidadDocente(Integer docenteId, Integer anio) {
    return sesiones.findAllByDocenteIdAndHorarioAnio(docenteId, anio).stream()
        .filter(sesion -> sesion.estaVigenteEn(LocalDate.now(clock)))
        .map(
            sesion ->
                new DisponibilidadDocente(
                    sesion.getDia(),
                    sesion.getBloque().getHoraInicio(),
                    sesion.getBloque().getHoraFin(),
                    NombreSeccion.completa(
                        sesion.getHorario().getSeccion().getGrado().getNumero(),
                        sesion.getHorario().getSeccion().getCodigo(),
                        sesion.getHorario().getSeccion().getGrado().getNivelEducativo()),
                    sesion.getArea().getNombre()))
        .sorted(
            Comparator.comparing(DisponibilidadDocente::dia)
                .thenComparing(DisponibilidadDocente::horaInicio))
        .toList();
  }

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

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public SesionHorarioResponse agregarSesion(Integer horarioId, SesionHorarioRequest request) {
    HorarioSeccion horario = buscar(horarioId);
    SesionHorario sesion = new SesionHorario();
    sesion.setHorario(horario);
    LocalDate primerDiaAnio = LocalDate.of(horario.getAnio(), 1, 1);
    sesion.setVigenteDesde(
        primerDiaAnio.isAfter(LocalDate.now(clock)) ? primerDiaAnio : LocalDate.now(clock));
    aplicar(horario, sesion, request, fechaDeConsulta(horario));
    sesion = sesiones.save(sesion);
    generacionSesiones.generarParaHorario(horario.getSeccion().getId(), horario.getAnio());
    auditoria.registrar(
        usuarioActual(),
        horario,
        "CREACIÓN",
        null,
        sesion,
        "PERMANENTE",
        sesion.getVigenteDesde(),
        null);
    return respuesta(sesion);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public SesionHorarioResponse actualizarSesion(
      Integer horarioId, Integer sesionId, SesionHorarioRequest request) {
    HorarioSeccion horario = buscar(horarioId);
    SesionHorario sesion = buscarSesion(horarioId, sesionId);
    LocalDate hoy = LocalDate.now(clock);
    if ("TEMPORAL".equalsIgnoreCase(request.tipoVigencia())) {
      return actualizarTemporalmente(horario, sesion, request, hoy);
    }
    LocalDate ultimoDiaDictado = ultimoDiaDictado(sesion);
    if (sesion.getVigenteDesde().isAfter(ultimoDiaDictado)) {
      SesionHorario anterior = copia(sesion);
      aplicar(horario, sesion, request, fechaDeConsulta(horario));
      borrarOcurrencias(sesion);
      sesion = sesiones.save(sesion);
      generacionSesiones.generarParaHorario(horario.getSeccion().getId(), horario.getAnio());
      auditoria.registrar(
          usuarioActual(),
          horario,
          "ACTUALIZACIÓN",
          anterior,
          sesion,
          "PERMANENTE",
          sesion.getVigenteDesde(),
          sesion.getVigenteHasta());
      return respuesta(sesion);
    }

    LocalDate vigenciaNueva = ultimoDiaDictado.plusDays(1);
    if (vigenciaNueva.getYear() != horario.getAnio()) {
      throw new ConflictException(
          "La asignación tiene sesiones fechadas; los cambios para el próximo año requieren un horario nuevo");
    }
    LocalDate finAnterior = sesion.getVigenteHasta();
    SesionHorario anterior = copia(sesion);
    sesion.setVigenteHasta(ultimoDiaDictado);
    sesiones.save(sesion);

    SesionHorario nueva = new SesionHorario();
    nueva.setHorario(horario);
    nueva.setVigenteDesde(vigenciaNueva);
    nueva.setVigenteHasta(finAnterior);
    aplicar(horario, nueva, request, vigenciaNueva);
    nueva = sesiones.save(nueva);
    actualizarOcurrenciasFuturas(sesion, ultimoDiaDictado, nueva);
    generacionSesiones.generarParaHorario(horario.getSeccion().getId(), horario.getAnio());
    auditoria.registrar(
        usuarioActual(),
        horario,
        "ACTUALIZACIÓN",
        anterior,
        nueva,
        "PERMANENTE",
        vigenciaNueva,
        nueva.getVigenteHasta());
    return respuesta(nueva);
  }

  private SesionHorarioResponse actualizarTemporalmente(
      HorarioSeccion horario, SesionHorario sesion, SesionHorarioRequest request, LocalDate hoy) {
    LocalDate desde = request.vigenteDesde();
    LocalDate hasta = request.vigenteHasta();
    LocalDate inicioAnio = LocalDate.of(horario.getAnio(), 1, 1);
    LocalDate finAnio = LocalDate.of(horario.getAnio(), 12, 31);
    if (desde == null || hasta == null || desde.isAfter(hasta)) {
      throw new BusinessException("Indica un rango válido para la vigencia temporal");
    }
    if (!desde.isAfter(hoy) || desde.isBefore(inicioAnio) || hasta.isAfter(finAnio)) {
      throw new BusinessException(
          "La vigencia temporal debe ser futura y estar dentro del año del horario");
    }
    if (!sesion.getVigenteDesde().isBefore(desde)
        || (sesion.getVigenteHasta() != null
            && sesion.getVigenteHasta().isBefore(hasta.plusDays(1)))) {
      throw new BusinessException(
          "El rango debe estar cubierto por la vigencia actual de la asignación");
    }

    SesionHorario anterior = copia(sesion);
    LocalDate finAnterior = sesion.getVigenteHasta();
    sesion.setVigenteHasta(desde.minusDays(1));
    sesiones.save(sesion);

    SesionHorario temporal = new SesionHorario();
    temporal.setHorario(horario);
    temporal.setVigenteDesde(desde);
    temporal.setVigenteHasta(hasta);
    aplicar(horario, temporal, request, desde);
    temporal = sesiones.save(temporal);

    SesionHorario restaurada = copia(anterior);
    restaurada.setVigenteDesde(hasta.plusDays(1));
    restaurada.setVigenteHasta(finAnterior);
    restaurada = sesiones.save(restaurada);
    // Las clases se buscan con la sesión persistida: la copia "anterior" no tiene id.
    actualizarOcurrenciasTemporales(sesion, temporal, restaurada, desde, hasta, hoy);
    auditoria.registrar(
        usuarioActual(), horario, "ACTUALIZACIÓN", anterior, temporal, "TEMPORAL", desde, hasta);
    return respuesta(temporal);
  }

  private void actualizarOcurrenciasTemporales(
      SesionHorario original,
      SesionHorario temporal,
      SesionHorario restaurada,
      LocalDate desde,
      LocalDate hasta,
      LocalDate hoy) {
    for (var ocurrencia :
        sesionesClase.findAllBySesionHorarioIdAndFechaAfterAndActiveTrue(original.getId(), hoy)) {
      LocalDate fecha = ocurrencia.getFecha();
      if (!fecha.isAfter(hasta)
          && asistencias.existsBySesionClaseIdAndActiveTrue(ocurrencia.getId())) {
        throw new ConflictException(
            "No se puede cambiar el horario: ya hay asistencia registrada en una sesión futura");
      }
      if (!fecha.isBefore(desde) && !fecha.isAfter(hasta)) {
        if (fecha.getDayOfWeek() == temporal.getDia()) {
          vincularOcurrencia(ocurrencia, temporal);
        } else {
          ocurrencia.setCancelada(true);
          ocurrencia.setMotivoCancelacion("La asignación temporal cambió antes de la fecha");
        }
      } else if (fecha.isAfter(hasta)) {
        if (fecha.getDayOfWeek() == restaurada.getDia()) {
          vincularOcurrencia(ocurrencia, restaurada);
        } else {
          ocurrencia.setCancelada(true);
          ocurrencia.setMotivoCancelacion("La asignación semanal cambió antes de la fecha");
        }
      }
    }
  }

  private void vincularOcurrencia(
      pe.edu.pucp.colegio.common.model.SesionClase ocurrencia, SesionHorario sesion) {
    ocurrencia.setSesionHorario(sesion);
    ocurrencia.setArea(sesion.getArea());
    ocurrencia.setDocente(sesion.getDocente());
    ocurrencia.setHoraFin(null);
    ocurrencia.setHoraInicio(sesion.getBloque().getHoraInicio());
    ocurrencia.setHoraFin(sesion.getBloque().getHoraFin());
  }

  private void actualizarOcurrenciasFuturas(
      SesionHorario sesionAnterior, LocalDate hoy, SesionHorario sesionNueva) {
    Docente docenteNuevo = sesionNueva.getDocente();
    for (var ocurrencia :
        sesionesClase.findAllBySesionHorarioIdAndFechaAfterAndActiveTrue(
            sesionAnterior.getId(), hoy)) {
      if (asistencias.existsBySesionClaseIdAndActiveTrue(ocurrencia.getId())) {
        throw new ConflictException(
            "No se puede cambiar la asignación: ya hay asistencia registrada en una sesión futura");
      }
      if (ocurrencia.getFecha().getDayOfWeek() == sesionNueva.getDia()) {
        ocurrencia.setSesionHorario(sesionNueva);
        ocurrencia.setArea(sesionNueva.getArea());
        ocurrencia.setDocente(docenteNuevo);
        ocurrencia.setHoraFin(null);
        ocurrencia.setHoraInicio(sesionNueva.getBloque().getHoraInicio());
        ocurrencia.setHoraFin(sesionNueva.getBloque().getHoraFin());
      } else if (!Boolean.TRUE.equals(ocurrencia.getCancelada())) {
        ocurrencia.setCancelada(true);
        ocurrencia.setMotivoCancelacion("La asignación semanal cambió antes de la fecha");
      }
    }
  }

  @Transactional
  public void eliminarSesion(Integer horarioId, Integer sesionId) {
    HorarioSeccion horario = buscar(horarioId);
    SesionHorario sesion = buscarSesion(horarioId, sesionId);
    SesionHorario anterior = copia(sesion);
    LocalDate ultimoDiaDictado = ultimoDiaDictado(sesion);
    if (!sesion.getVigenteDesde().isAfter(ultimoDiaDictado)) {
      sesion.setVigenteHasta(ultimoDiaDictado);
      sesiones.save(sesion);
      for (var ocurrencia :
          sesionesClase.findAllBySesionHorarioIdAndFechaAfterAndActiveTrue(
              sesion.getId(), ultimoDiaDictado)) {
        ocurrencia.setCancelada(true);
        ocurrencia.setMotivoCancelacion("La asignación semanal fue retirada");
      }
      auditoria.registrar(
          usuarioActual(),
          horario,
          "ELIMINACIÓN",
          anterior,
          null,
          "PERMANENTE",
          sesion.getVigenteHasta().plusDays(1),
          null);
      return;
    }
    boolean teniaClases = borrarOcurrencias(sesion);
    sesiones.delete(sesion);
    LocalDate hoy = LocalDate.now(clock);
    auditoria.registrar(
        usuarioActual(),
        horario,
        "ELIMINACIÓN",
        anterior,
        null,
        "PERMANENTE",
        teniaClases && sesion.getVigenteDesde().isAfter(hoy) ? sesion.getVigenteDesde() : hoy,
        null);
  }

  /**
   * Último día que conserva la versión actual de la asignación: hoy si la clase de hoy ya empezó o
   * tiene asistencia; si no, ayer, y el cambio rige desde hoy mismo.
   */
  private LocalDate ultimoDiaDictado(SesionHorario sesion) {
    LocalDateTime ahora = LocalDateTime.now(clock);
    LocalDate hoy = ahora.toLocalDate();
    boolean claseDeHoyIniciada =
        sesionesClase.findAllBySesionHorarioIdAndFechaAndActiveTrue(sesion.getId(), hoy).stream()
            .filter(ocurrencia -> !Boolean.TRUE.equals(ocurrencia.getCancelada()))
            .anyMatch(
                ocurrencia ->
                    !ocurrencia.getHoraInicio().isAfter(ahora.toLocalTime())
                        || asistencias.existsBySesionClaseIdAndActiveTrue(ocurrencia.getId()));
    return claseDeHoyIniciada ? hoy : hoy.minusDays(1);
  }

  /**
   * Quita las clases fechadas de una asignación que todavía no se dictó; indica si tenía alguna.
   */
  private boolean borrarOcurrencias(SesionHorario sesion) {
    var ocurrencias = sesionesClase.findAllBySesionHorarioId(sesion.getId());
    for (var ocurrencia : ocurrencias) {
      if (asistencias.existsBySesionClaseId(ocurrencia.getId())) {
        throw new ConflictException(
            "No se puede cambiar la asignación: ya hay asistencia registrada en una de sus clases");
      }
    }
    sesionesClase.deleteAll(ocurrencias);
    sesionesClase.flush();
    return !ocurrencias.isEmpty();
  }

  private SesionHorario copia(SesionHorario original) {
    SesionHorario copia = new SesionHorario();
    copia.setHorario(original.getHorario());
    copia.setBloque(original.getBloque());
    copia.setDia(original.getDia());
    copia.setArea(original.getArea());
    copia.setDocente(original.getDocente());
    copia.setAula(original.getAula());
    copia.setVigenteDesde(original.getVigenteDesde());
    copia.setVigenteHasta(original.getVigenteHasta());
    return copia;
  }

  private String usuarioActual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth == null ? "sistema" : auth.getName();
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
        detectarConflictos(
            horario,
            datos,
            request.dia(),
            excluirSesionId,
            "TEMPORAL".equalsIgnoreCase(request.tipoVigencia()) && request.vigenteDesde() != null
                ? request.vigenteDesde()
                : fechaDeConsulta(horario));
    return new ValidacionSesionResponse(conflictos.isEmpty(), conflictos);
  }

  private void aplicar(
      HorarioSeccion horario,
      SesionHorario sesion,
      SesionHorarioRequest request,
      LocalDate fechaEfectiva) {
    Resolucion datos = resolver(horario, request);
    // Serializa las comprobaciones sobre grilla, docente y aula ante escrituras concurrentes.
    horarios.findByIdForUpdate(horario.getId());
    docentes.findByIdForUpdate(datos.docente().getId());
    aulas.findByIdForUpdate(datos.aula().getId());
    List<ConflictoSesionResponse> conflictos =
        detectarConflictos(horario, datos, request.dia(), sesion.getId(), fechaEfectiva);
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
    // docenteId es docentes.id, igual que en sesiones de clase y asistencia docente. Ademas del
    // perfil activo, la persona debe tener una cuenta DOCENTE activa.
    Docente docente =
        docentes
            .findById(request.docenteId())
            .filter(d -> Boolean.TRUE.equals(d.getActive()))
            .filter(
                d ->
                    cuentas.existsByPersonaIdAndRolAndActiveTrue(
                        d.getPersona().getId(), Rol.DOCENTE))
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
      HorarioSeccion horario,
      Resolucion datos,
      DayOfWeek dia,
      Integer sesionIdExcluida,
      LocalDate fechaEfectiva) {
    List<ConflictoSesionResponse> conflictos = new ArrayList<>();
    BloqueHorario bloque = datos.bloque();
    int excluir = sesionIdExcluida == null ? SIN_EXCLUIR : sesionIdExcluida;

    boolean ocupada =
        sesiones.existeCeldaVigente(
            horario.getId(), bloque.getId(), dia, fechaEfectiva, sesionIdExcluida);
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
            fechaEfectiva,
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
            fechaEfectiva,
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
    long capacidadRequerida =
        seccion.getVacantesTotales() == null ? 0 : Math.max(seccion.getVacantesTotales(), alumnos);
    if (capacidadRequerida > datos.aula().getCapacidad()) {
      conflictos.add(
          new ConflictoSesionResponse(
              TipoConflictoHorario.CAPACIDAD,
              "El aula "
                  + datos.aula().getCodigo()
                  + " tiene capacidad para "
                  + datos.aula().getCapacidad()
                  + " alumnos y la sección "
                  + nombreSeccion(seccion)
                  + " requiere espacio para "
                  + capacidadRequerida
                  + " ("
                  + alumnos
                  + " matriculados) en "
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
            .filter(s -> s.estaVigenteEn(fechaDeConsulta(horario)))
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
    Docente docente = sesion.getDocente();
    return new SesionHorarioResponse(
        sesion.getId(),
        sesion.getBloque().getId(),
        sesion.getDia(),
        sesion.getArea().getCodigo(),
        sesion.getArea().getNombre(),
        docente.getId(),
        nombreDocente(docente),
        sesion.getAula().getId(),
        sesion.getAula().getCodigo(),
        sesion.getVigenteDesde(),
        sesion.getVigenteHasta());
  }

  private LocalDate fechaDeConsulta(HorarioSeccion horario) {
    LocalDate hoy = LocalDate.now(clock);
    LocalDate inicioAnio = LocalDate.of(horario.getAnio(), 1, 1);
    LocalDate finAnio = LocalDate.of(horario.getAnio(), 12, 31);
    if (hoy.isBefore(inicioAnio)) {
      return inicioAnio;
    }
    return hoy.isAfter(finAnio) ? finAnio : hoy;
  }

  private String nombreDocente(Docente docente) {
    var persona = docente.getPersona();
    return (persona.getNombre() + " " + persona.getApellidoPaterno()).trim();
  }

  private String nombreSeccion(Seccion seccion) {
    return NombreSeccion.completa(
        seccion.getGrado().getNumero(),
        seccion.getCodigo(),
        seccion.getGrado().getNivelEducativo());
  }

  private String nombreDia(DayOfWeek dia) {
    return dia.getDisplayName(TextStyle.FULL, ES);
  }

  private String franja(BloqueHorario bloque) {
    return bloque.getHoraInicio() + " a " + bloque.getHoraFin();
  }
}
