package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.pucp.colegio.common.dto.AsistenciaDocenteCreateRequest;
import pe.edu.pucp.colegio.common.dto.CargaMasivaAsistenciaResultadoDTO;
import pe.edu.pucp.colegio.common.dto.FilaAsistenciaDocenteCsv;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;
import pe.edu.pucp.colegio.common.repository.AsistenciaDocenteRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.security.dto.ErrorFilaDTO;

/**
 * Carga masiva de asistencia docente desde CSV (US-2.5). Primero valida todas las filas sin tocar
 * la base; solo si ninguna tiene errores (y no se pidió únicamente validar) guarda el lote
 * completo.
 */
@Service
public class CargaMasivaAsistenciaDocenteService {

  private static final Logger log =
      LoggerFactory.getLogger(CargaMasivaAsistenciaDocenteService.class);

  private static final Pattern DNI = Pattern.compile("\\d{8}");
  private static final DateTimeFormatter FECHA_LATINA = DateTimeFormatter.ofPattern("d/M/uuuu");
  private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("H:mm[:ss]");
  private static final int MAX_MOTIVO = 500;

  private final LectorCsvAsistencia lector;
  private final DocenteRepository docentes;
  private final AsistenciaDocenteRepository asistencias;
  private final AsistenciaDocenteService asistenciaService;
  private final Clock clock;

  public CargaMasivaAsistenciaDocenteService(
      LectorCsvAsistencia lector,
      DocenteRepository docentes,
      AsistenciaDocenteRepository asistencias,
      AsistenciaDocenteService asistenciaService,
      Clock clock) {
    this.lector = lector;
    this.docentes = docentes;
    this.asistencias = asistencias;
    this.asistenciaService = asistenciaService;
    this.clock = clock;
  }

  public CargaMasivaAsistenciaResultadoDTO cargar(
      byte[] contenido, boolean soloValidar, String usuario) {
    List<FilaAsistenciaDocenteCsv> filas = lector.leerDocentes(contenido);
    List<ErrorFilaDTO> errores = new ArrayList<>();
    List<AsistenciaDocenteCreateRequest> validas = new ArrayList<>();
    Set<String> vistos = new HashSet<>();
    int nuevos = 0;
    int actualizados = 0;

    for (FilaAsistenciaDocenteCsv fila : filas) {
      int erroresPrevios = errores.size();
      AsistenciaDocenteCreateRequest solicitud = validarFila(fila, errores);
      if (errores.size() > erroresPrevios) {
        continue;
      }
      if (!vistos.add(solicitud.docenteId() + "|" + solicitud.fecha())) {
        errores.add(
            new ErrorFilaDTO(
                fila.fila(), "fecha", "El docente ya aparece con esa fecha en otra fila"));
        continue;
      }
      validas.add(solicitud);
      if (asistencias.existsByDocenteIdAndFechaAndActiveTrue(
          solicitud.docenteId(), solicitud.fecha())) {
        actualizados++;
      } else {
        nuevos++;
      }
    }

    boolean guardar = errores.isEmpty() && !soloValidar;
    if (guardar) {
      asistenciaService.registrarOActualizarLote(validas);
    }
    log.info(
        "Carga masiva de asistencia docente por {}: filas={}, errores={}, guardado={}",
        usuario,
        filas.size(),
        errores.size(),
        guardar);
    return new CargaMasivaAsistenciaResultadoDTO(
        guardar,
        soloValidar,
        filas.size(),
        validas.size(),
        (int) errores.stream().map(ErrorFilaDTO::fila).distinct().count(),
        nuevos,
        actualizados,
        errores);
  }

  /** Devuelve la solicitud de la fila; los problemas se agregan a {@code errores}. */
  private AsistenciaDocenteCreateRequest validarFila(
      FilaAsistenciaDocenteCsv fila, List<ErrorFilaDTO> errores) {
    int n = fila.fila();
    Docente docente = buscarDocente(fila.dniDocente(), n, errores);
    LocalDate fecha = leerFecha(fila.fecha(), n, errores);
    LocalTime entrada = leerHora(fila.horaEntrada(), "hora_entrada", n, errores);
    LocalTime salida = leerHora(fila.horaSalida(), "hora_salida", n, errores);
    EstadoAsistenciaDocente estado =
        leerEstado(fila.estado(), fila.horaEntrada() != null, n, errores);
    String motivo = fila.motivo();

    if (motivo != null && motivo.length() > MAX_MOTIVO) {
      errores.add(
          new ErrorFilaDTO(
              n, "motivo", "El motivo no puede superar " + MAX_MOTIVO + " caracteres"));
    }
    if (fila.horaSalida() != null && fila.horaEntrada() == null) {
      errores.add(new ErrorFilaDTO(n, "hora_salida", "Debe indicar la hora de entrada"));
    }
    if (entrada != null && salida != null && salida.isBefore(entrada)) {
      errores.add(
          new ErrorFilaDTO(n, "hora_salida", "La salida no puede ser anterior a la entrada"));
    }
    if (estado == EstadoAsistenciaDocente.INASISTENCIA
        && (fila.horaEntrada() != null || fila.horaSalida() != null)) {
      errores.add(new ErrorFilaDTO(n, "estado", "Una inasistencia no puede incluir horas"));
    }
    if ((estado == EstadoAsistenciaDocente.PRESENTE || estado == EstadoAsistenciaDocente.TARDANZA)
        && fila.horaEntrada() == null) {
      errores.add(
          new ErrorFilaDTO(n, "hora_entrada", "La hora de entrada es obligatoria si asistió"));
    }
    if ((estado == EstadoAsistenciaDocente.INASISTENCIA
            || estado == EstadoAsistenciaDocente.TARDANZA)
        && motivo == null) {
      errores.add(new ErrorFilaDTO(n, "motivo", "El motivo es obligatorio para este estado"));
    }
    if (docente == null || fecha == null || estado == null) {
      return null;
    }
    return new AsistenciaDocenteCreateRequest(
        docente.getId(), fecha, estado, entrada, salida, motivo);
  }

  private Docente buscarDocente(String dni, int fila, List<ErrorFilaDTO> errores) {
    if (dni == null || !DNI.matcher(dni).matches()) {
      errores.add(new ErrorFilaDTO(fila, "dni_docente", "El DNI debe tener 8 dígitos"));
      return null;
    }
    return docentes
        .findByPersonaDniAndActiveTrue(dni)
        .orElseGet(
            () -> {
              errores.add(
                  new ErrorFilaDTO(fila, "dni_docente", "No existe un docente activo con ese DNI"));
              return null;
            });
  }

  private LocalDate leerFecha(String texto, int fila, List<ErrorFilaDTO> errores) {
    if (texto == null) {
      errores.add(new ErrorFilaDTO(fila, "fecha", "La fecha es obligatoria"));
      return null;
    }
    LocalDate fecha = parsearFecha(texto);
    if (fecha == null) {
      errores.add(new ErrorFilaDTO(fila, "fecha", "Use el formato AAAA-MM-DD o DD/MM/AAAA"));
    } else if (fecha.isAfter(LocalDate.now(clock))) {
      errores.add(new ErrorFilaDTO(fila, "fecha", "La fecha no puede ser posterior a hoy"));
      return null;
    }
    return fecha;
  }

  private static LocalDate parsearFecha(String texto) {
    try {
      return LocalDate.parse(texto);
    } catch (DateTimeParseException e) {
      try {
        return LocalDate.parse(texto, FECHA_LATINA);
      } catch (DateTimeParseException ignorada) {
        return null;
      }
    }
  }

  private static LocalTime leerHora(
      String texto, String campo, int fila, List<ErrorFilaDTO> errores) {
    if (texto == null) {
      return null;
    }
    try {
      return LocalTime.parse(texto, HORA);
    } catch (DateTimeParseException e) {
      errores.add(new ErrorFilaDTO(fila, campo, "Use el formato HH:MM (24 horas)"));
      return null;
    }
  }

  /** Sin estado: con hora de entrada es presente (la tardanza se calcula al guardar). */
  private static EstadoAsistenciaDocente leerEstado(
      String texto, boolean hayEntrada, int fila, List<ErrorFilaDTO> errores) {
    if (texto == null) {
      return hayEntrada ? EstadoAsistenciaDocente.PRESENTE : EstadoAsistenciaDocente.INASISTENCIA;
    }
    try {
      EstadoAsistenciaDocente estado =
          EstadoAsistenciaDocente.valueOf(texto.toUpperCase(Locale.ROOT));
      if (estado != EstadoAsistenciaDocente.PENDIENTE_MARCAJE) {
        return estado;
      }
    } catch (IllegalArgumentException e) {
      // Se informa abajo junto con los estados permitidos.
    }
    errores.add(
        new ErrorFilaDTO(fila, "estado", "Estados permitidos: PRESENTE, TARDANZA, INASISTENCIA"));
    return null;
  }
}
