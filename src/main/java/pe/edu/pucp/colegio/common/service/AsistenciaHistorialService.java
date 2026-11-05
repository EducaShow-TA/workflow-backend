package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AsistenciaHistorialResponse;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;

/** Consulta el historial con permisos de alumno, apoderado o personal autorizado. */
@Service
public class AsistenciaHistorialService {

  private final AsistenciaEstudianteRepository asistencias;
  private final AccesoAsistenciaService acceso;

  public AsistenciaHistorialService(
      AsistenciaEstudianteRepository asistencias, AccesoAsistenciaService acceso) {
    this.asistencias = asistencias;
    this.acceso = acceso;
  }

  @Transactional(readOnly = true)
  public List<AsistenciaHistorialResponse> consultar(
      Integer estudianteId, LocalDate desde, LocalDate hasta, String username) {
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
    }
    acceso.exigirAccesoHistorial(estudianteId, username);
    return asistencias.buscarHistorial(estudianteId, desde, hasta).stream()
        .map(this::respuesta)
        .toList();
  }

  private AsistenciaHistorialResponse respuesta(AsistenciaEstudiante asistencia) {
    var sesion = asistencia.getSesionClase();
    String documentoUrl =
        asistencia.getArchivoJustificacion() == null
            ? null
            : "/api/v1/asistencias-estudiantes/" + asistencia.getId() + "/justificacion/documento";
    return new AsistenciaHistorialResponse(
        asistencia.getId(),
        sesion.getFecha(),
        sesion.getHoraInicio(),
        sesion.getHoraFin(),
        sesion.getSeccion().getId(),
        sesion.getSeccion().getCodigo(),
        sesion.getArea().getCodigo(),
        sesion.getArea().getNombre(),
        sesion.getDocente().getPersona().getNombreCompleto(),
        asistencia.getEstadoAsistencia(),
        asistencia.getMotivoJustificacion(),
        asistencia.getDocumentoJustificacionNombre(),
        documentoUrl);
  }
}
