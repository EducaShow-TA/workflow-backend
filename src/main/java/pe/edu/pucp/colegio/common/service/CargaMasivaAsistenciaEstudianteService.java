package pe.edu.pucp.colegio.common.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.pucp.colegio.common.dto.AsistenciaEstudianteRequest;
import pe.edu.pucp.colegio.common.dto.CargaMasivaAsistenciaResultadoDTO;
import pe.edu.pucp.colegio.common.dto.FilaAsistenciaEstudianteCsv;
import pe.edu.pucp.colegio.common.dto.FilaListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.ListaAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.RegistroAsistenciaSesionRequest;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.security.dto.ErrorFilaDTO;

/**
 * Carga masiva de la asistencia de alumnos de una sesión desde CSV (US-2.2). Informa los errores
 * fila por fila antes de guardar y, si no hay ninguno, guarda con las mismas reglas de la toma
 * manual (ventana de marcado, docente de la sesión y presencia del docente).
 */
@Service
public class CargaMasivaAsistenciaEstudianteService {

  private static final Logger log =
      LoggerFactory.getLogger(CargaMasivaAsistenciaEstudianteService.class);

  private static final Pattern DNI = Pattern.compile("\\d{8}");

  private final LectorCsvAsistencia lector;
  private final SesionClaseService sesiones;

  public CargaMasivaAsistenciaEstudianteService(
      LectorCsvAsistencia lector, SesionClaseService sesiones) {
    this.lector = lector;
    this.sesiones = sesiones;
  }

  public CargaMasivaAsistenciaResultadoDTO cargar(
      Integer sesionId, byte[] contenido, boolean soloValidar, SesionClaseService.Acceso acceso) {
    List<FilaAsistenciaEstudianteCsv> filas = lector.leerAlumnos(contenido);
    ListaAsistenciaResponse lista = sesiones.obtenerListaParaCarga(sesionId, acceso);
    Map<String, FilaListaAsistenciaResponse> matriculados = new HashMap<>();
    for (FilaListaAsistenciaResponse alumno : lista.alumnos()) {
      if (alumno.matriculado()) {
        matriculados.put(alumno.dni(), alumno);
      }
    }

    List<ErrorFilaDTO> errores = new ArrayList<>();
    List<AsistenciaEstudianteRequest> validas = new ArrayList<>();
    Set<String> vistos = new HashSet<>();
    int nuevos = 0;
    int actualizados = 0;

    for (FilaAsistenciaEstudianteCsv fila : filas) {
      int erroresPrevios = errores.size();
      FilaListaAsistenciaResponse alumno = buscarAlumno(fila, matriculados, vistos, errores);
      EstadoAsistencia estado = leerEstado(fila, errores);
      if (alumno != null
          && alumno.estado() == EstadoAsistencia.JUSTIFICADA
          && errores.size() == erroresPrevios) {
        errores.add(
            new ErrorFilaDTO(
                fila.fila(),
                "estado",
                "La asistencia del alumno está justificada y no se puede cambiar por CSV"));
      }
      if (errores.size() > erroresPrevios) {
        continue;
      }
      validas.add(new AsistenciaEstudianteRequest(alumno.estudianteId(), estado));
      if (alumno.estado() == null) {
        nuevos++;
      } else {
        actualizados++;
      }
    }

    boolean guardar = errores.isEmpty() && !soloValidar;
    if (guardar) {
      sesiones.guardarAsistencias(sesionId, new RegistroAsistenciaSesionRequest(validas), acceso);
    }
    log.info(
        "Carga masiva de asistencia de alumnos en la sesión {} por {}: filas={}, errores={},"
            + " guardado={}",
        sesionId,
        acceso.usuario(),
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

  private FilaListaAsistenciaResponse buscarAlumno(
      FilaAsistenciaEstudianteCsv fila,
      Map<String, FilaListaAsistenciaResponse> matriculados,
      Set<String> vistos,
      List<ErrorFilaDTO> errores) {
    String dni = fila.dniAlumno();
    if (dni == null || !DNI.matcher(dni).matches()) {
      errores.add(new ErrorFilaDTO(fila.fila(), "dni_alumno", "El DNI debe tener 8 dígitos"));
      return null;
    }
    FilaListaAsistenciaResponse alumno = matriculados.get(dni);
    if (alumno == null) {
      errores.add(
          new ErrorFilaDTO(
              fila.fila(), "dni_alumno", "El alumno no está matriculado en esta sección"));
      return null;
    }
    if (!vistos.add(dni)) {
      errores.add(new ErrorFilaDTO(fila.fila(), "dni_alumno", "El alumno ya aparece en otra fila"));
      return null;
    }
    return alumno;
  }

  private static EstadoAsistencia leerEstado(
      FilaAsistenciaEstudianteCsv fila, List<ErrorFilaDTO> errores) {
    if (fila.estado() != null) {
      try {
        EstadoAsistencia estado = EstadoAsistencia.valueOf(fila.estado().toUpperCase(Locale.ROOT));
        if (estado == EstadoAsistencia.PRESENTE
            || estado == EstadoAsistencia.TARDANZA
            || estado == EstadoAsistencia.AUSENTE) {
          return estado;
        }
      } catch (IllegalArgumentException e) {
        // Se informa abajo junto con los estados permitidos.
      }
    }
    errores.add(
        new ErrorFilaDTO(fila.fila(), "estado", "Estados permitidos: PRESENTE, TARDANZA, AUSENTE"));
    return null;
  }
}
