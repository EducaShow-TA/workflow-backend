package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.DiaNoLectivoRequest;
import pe.edu.pucp.colegio.common.dto.DiaNoLectivoResponse;
import pe.edu.pucp.colegio.common.dto.GeneracionSesionesResponse;
import pe.edu.pucp.colegio.common.model.DiaNoLectivo;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.model.SesionHorario;
import pe.edu.pucp.colegio.common.repository.DiaNoLectivoRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.HorarioSeccionRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;

/** Materializa como ocurrencias fechadas las asignaciones semanales del periodo. */
@Service
public class GeneracionSesionClaseService {

  private final PeriodoRepository periodos;
  private final HorarioSeccionRepository horarios;
  private final SesionHorarioRepository asignaciones;
  private final SesionClaseRepository sesiones;
  private final DiaNoLectivoRepository diasNoLectivos;
  private final DocenteRepository docentes;
  private final Clock clock;

  public GeneracionSesionClaseService(
      PeriodoRepository periodos,
      HorarioSeccionRepository horarios,
      SesionHorarioRepository asignaciones,
      SesionClaseRepository sesiones,
      DiaNoLectivoRepository diasNoLectivos,
      DocenteRepository docentes,
      Clock clock) {
    this.periodos = periodos;
    this.horarios = horarios;
    this.asignaciones = asignaciones;
    this.sesiones = sesiones;
    this.diasNoLectivos = diasNoLectivos;
    this.docentes = docentes;
    this.clock = clock;
  }

  @Transactional
  public GeneracionSesionesResponse generarPeriodo(Integer periodoId) {
    Periodo periodo =
        periodos
            .findById(periodoId)
            .filter(p -> Boolean.TRUE.equals(p.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo no encontrado"));
    HorarioSeccion horario =
        horarios
            .findBySeccionIdAndAnio(
                periodo.getSeccion().getId(), periodo.getFechaInicio().getYear())
            .filter(h -> Boolean.TRUE.equals(h.getActive()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La sección no tiene horario para el año del periodo"));

    LocalDate hoy = LocalDate.now(clock);
    LocalDate desde = periodo.getFechaInicio().isAfter(hoy) ? periodo.getFechaInicio() : hoy;
    if (desde.isAfter(periodo.getFechaFin())) {
      return new GeneracionSesionesResponse(periodoId, 0, 0, 0);
    }
    Map<LocalDate, DiaNoLectivo> cierres =
        diasNoLectivos
            .findAllByActiveTrueAndFechaBetweenOrderByFecha(desde, periodo.getFechaFin())
            .stream()
            .collect(Collectors.toMap(DiaNoLectivo::getFecha, Function.identity()));

    int creadas = 0;
    int canceladas = 0;
    int omitidas = 0;
    List<SesionHorario> celdas = asignaciones.findAllByHorarioId(horario.getId());
    for (LocalDate fecha = desde;
        !fecha.isAfter(periodo.getFechaFin());
        fecha = fecha.plusDays(1)) {
      DiaNoLectivo cierre = cierres.get(fecha);
      for (SesionHorario celda : celdas) {
        if (celda.getDia() != fecha.getDayOfWeek() || !celda.estaVigenteEn(fecha)) {
          continue;
        }
        List<SesionClase> existentes =
            sesiones.findAllBySesionHorarioIdAndFechaAndActiveTrue(celda.getId(), fecha);
        if (cierre != null) {
          if (existentes.isEmpty()) {
            omitidas++;
          } else {
            for (SesionClase existente : existentes) {
              if (!Boolean.TRUE.equals(existente.getCancelada())) {
                existente.setCancelada(true);
                existente.setMotivoCancelacion(cierre.getDescripcion());
                canceladas++;
              }
            }
          }
          continue;
        }
        if (sesiones.existsBySesionHorarioIdAndFecha(celda.getId(), fecha)) {
          continue;
        }
        Docente docente =
            docentes
                .findByPersonaId(celda.getDocente().getPersona().getId())
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "La cuenta asignada no tiene un perfil docente activo"));
        SesionClase sesion = new SesionClase();
        sesion.setSeccion(horario.getSeccion());
        sesion.setArea(celda.getArea());
        sesion.setDocente(docente);
        sesion.setSesionHorario(celda);
        sesion.setFecha(fecha);
        sesion.setHoraInicio(celda.getBloque().getHoraInicio());
        sesion.setHoraFin(celda.getBloque().getHoraFin());
        sesiones.save(sesion);
        creadas++;
      }
    }
    return new GeneracionSesionesResponse(periodoId, creadas, canceladas, omitidas);
  }

  @Transactional
  public DiaNoLectivoResponse crearDiaNoLectivo(DiaNoLectivoRequest request) {
    if (request.fecha().isBefore(LocalDate.now(clock))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Los cambios retroactivos al calendario requieren una corrección auditada");
    }
    DiaNoLectivo dia = diasNoLectivos.findByFecha(request.fecha()).orElseGet(DiaNoLectivo::new);
    dia.setFecha(request.fecha());
    dia.setDescripcion(request.descripcion().trim());
    dia.setActive(true);
    try {
      dia = diasNoLectivos.saveAndFlush(dia);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe una fecha no lectiva para ese día", e);
    }
    for (SesionClase sesion :
        sesiones.findAllByFechaAndSesionHorarioIsNotNullAndActiveTrue(dia.getFecha())) {
      sesion.setCancelada(true);
      sesion.setMotivoCancelacion(dia.getDescripcion());
    }
    return respuesta(dia);
  }

  @Transactional
  public void eliminarDiaNoLectivo(Integer id) {
    DiaNoLectivo dia =
        diasNoLectivos
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fecha no encontrada"));
    dia.setActive(false);
    if (!dia.getFecha().isBefore(LocalDate.now(clock))) {
      for (SesionClase sesion :
          sesiones.findAllByFechaAndSesionHorarioIsNotNullAndActiveTrue(dia.getFecha())) {
        if (Boolean.TRUE.equals(sesion.getCancelada())
            && dia.getDescripcion().equals(sesion.getMotivoCancelacion())) {
          sesion.setCancelada(false);
          sesion.setMotivoCancelacion(null);
        }
      }
    }
  }

  @Transactional(readOnly = true)
  public List<DiaNoLectivoResponse> listarDiasNoLectivos(LocalDate desde, LocalDate hasta) {
    if (desde == null || hasta == null || hasta.isBefore(desde)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica un rango de fechas válido");
    }
    return diasNoLectivos.findAllByActiveTrueAndFechaBetweenOrderByFecha(desde, hasta).stream()
        .map(GeneracionSesionClaseService::respuesta)
        .toList();
  }

  private static DiaNoLectivoResponse respuesta(DiaNoLectivo dia) {
    return new DiaNoLectivoResponse(dia.getId(), dia.getFecha(), dia.getDescripcion());
  }
}
