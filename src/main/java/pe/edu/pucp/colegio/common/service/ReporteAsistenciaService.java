package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.FilaReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.ReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.SeccionReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.TotalesAsistenciaResponse;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.repository.ConsolidadoAsistenciaFila;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/**
 * Genera el reporte de inasistencias y tardanzas de alumnos (US-2.6). Cuenta sesiones de clase, no
 * días. El administrador ve todo; el docente, con la misma regla del historial de asistencia, solo
 * las sesiones que dicta o las de secciones donde tiene clase.
 */
@Service
public class ReporteAsistenciaService {

  private static final Logger log = LoggerFactory.getLogger(ReporteAsistenciaService.class);

  private static final Comparator<FilaReporteAsistenciaResponse> ORDEN_RIESGO =
      Comparator.comparing(FilaReporteAsistenciaResponse::porcentajeInasistencia)
          .reversed()
          .thenComparing(
              Comparator.comparingLong(FilaReporteAsistenciaResponse::inasistencias).reversed())
          .thenComparing(FilaReporteAsistenciaResponse::nombreCompleto);

  private static final Comparator<Seccion> ORDEN_SECCIONES =
      Comparator.comparing((Seccion s) -> s.getGrado().getNivelEducativo())
          .thenComparing(s -> s.getGrado().getNumero())
          .thenComparing(Seccion::getCodigo);

  private final AsistenciaEstudianteRepository asistencias;
  private final SesionClaseRepository sesiones;
  private final SeccionRepository secciones;
  private final CuentaUsuarioRepository cuentas;

  public ReporteAsistenciaService(
      AsistenciaEstudianteRepository asistencias,
      SesionClaseRepository sesiones,
      SeccionRepository secciones,
      CuentaUsuarioRepository cuentas) {
    this.asistencias = asistencias;
    this.sesiones = sesiones;
    this.secciones = secciones;
    this.cuentas = cuentas;
  }

  @Transactional(readOnly = true)
  public ReporteAsistenciaResponse generar(
      LocalDate desde, LocalDate hasta, Integer gradoId, Integer seccionId, String username) {
    if (desde.isAfter(hasta)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
    }
    Integer personaIdDocente = personaIdDocente(buscarCuenta(username));
    if (personaIdDocente != null && seccionId != null) {
      exigirClaseEnSeccion(personaIdDocente, seccionId, desde, hasta);
    }
    log.info(
        "Reporte de asistencia solicitado por {}: {} a {}, grado {}, sección {}",
        username,
        desde,
        hasta,
        gradoId,
        seccionId);

    List<FilaReporteAsistenciaResponse> alumnos =
        asistencias.consolidarPorAlumno(desde, hasta, gradoId, seccionId, personaIdDocente).stream()
            .map(this::fila)
            .sorted(ORDEN_RIESGO)
            .toList();
    return new ReporteAsistenciaResponse(desde, hasta, totales(alumnos), alumnos);
  }

  @Transactional(readOnly = true)
  public List<SeccionReporteAsistenciaResponse> listarSecciones(String username) {
    Integer personaIdDocente = personaIdDocente(buscarCuenta(username));
    List<Seccion> disponibles =
        personaIdDocente == null
            ? secciones.findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc()
            : secciones.buscarSeccionesDelDocente(personaIdDocente);
    return disponibles.stream()
        .sorted(ORDEN_SECCIONES)
        .map(
            s ->
                new SeccionReporteAsistenciaResponse(
                    s.getId(),
                    s.getCodigo(),
                    s.getGrado().getId(),
                    s.getGrado().getNumero(),
                    NombreSeccion.grado(
                        s.getGrado().getNumero(), s.getGrado().getNivelEducativo())))
        .toList();
  }

  /** Porcentaje de faltas sin justificar con un decimal; 0.0 si no hay sesiones contables. */
  public static BigDecimal porcentaje(long inasistencias, long totalSesiones) {
    if (totalSesiones == 0) {
      return BigDecimal.ZERO.setScale(1);
    }
    return BigDecimal.valueOf(inasistencias * 100)
        .divide(BigDecimal.valueOf(totalSesiones), 1, RoundingMode.HALF_UP);
  }

  private CuentaUsuario buscarCuenta(String username) {
    return cuentas
        .findByUsername(username)
        .filter(cuenta -> Boolean.TRUE.equals(cuenta.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no válida"));
  }

  /** {@code null} para el administrador; la persona del docente para filtrar sus sesiones. */
  private Integer personaIdDocente(CuentaUsuario cuenta) {
    if (cuenta.getRol() == Rol.ADMINISTRADOR) {
      return null;
    }
    if (cuenta.getRol() == Rol.DOCENTE && cuenta.getPersona() != null) {
      return cuenta.getPersona().getId();
    }
    throw prohibido();
  }

  private void exigirClaseEnSeccion(
      Integer personaId, Integer seccionId, LocalDate desde, LocalDate hasta) {
    boolean tieneClase =
        sesiones.docenteTieneClaseEnSeccion(personaId, seccionId, desde, hasta)
            || sesiones.existsByDocentePersonaIdAndSeccionIdAndFechaBetweenAndActiveTrue(
                personaId, seccionId, desde, hasta);
    if (!tieneClase) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "No tiene clases en esta sección en el rango de fechas seleccionado");
    }
  }

  private FilaReporteAsistenciaResponse fila(ConsolidadoAsistenciaFila f) {
    long total = f.getPresentes() + f.getTardanzas() + f.getInasistencias() + f.getJustificadas();
    return new FilaReporteAsistenciaResponse(
        f.getEstudianteId(),
        f.getNombre() + " " + f.getApellidoPaterno() + " " + f.getApellidoMaterno(),
        f.getGradoId(),
        f.getGradoNumero(),
        NombreSeccion.grado(f.getGradoNumero(), f.getNivelEducativo()),
        f.getSeccionId(),
        f.getSeccionCodigo(),
        f.getPresentes(),
        f.getTardanzas(),
        f.getInasistencias(),
        f.getJustificadas(),
        total,
        porcentaje(f.getInasistencias(), total));
  }

  private TotalesAsistenciaResponse totales(List<FilaReporteAsistenciaResponse> filas) {
    long presentes = 0;
    long tardanzas = 0;
    long inasistencias = 0;
    long justificadas = 0;
    for (FilaReporteAsistenciaResponse fila : filas) {
      presentes += fila.presentes();
      tardanzas += fila.tardanzas();
      inasistencias += fila.inasistencias();
      justificadas += fila.justificadas();
    }
    return new TotalesAsistenciaResponse(
        presentes,
        tardanzas,
        inasistencias,
        justificadas,
        presentes + tardanzas + inasistencias + justificadas);
  }

  private ResponseStatusException prohibido() {
    return new ResponseStatusException(
        HttpStatus.FORBIDDEN, "No tiene permiso para consultar el reporte de esta sección");
  }
}
