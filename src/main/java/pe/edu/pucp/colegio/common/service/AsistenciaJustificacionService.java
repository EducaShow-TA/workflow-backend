package pe.edu.pucp.colegio.common.service;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AsistenciaJustificacionResponse;
import pe.edu.pucp.colegio.common.model.AsistenciaEstudiante;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;

/** Almacena documentos de sustento y justifica inasistencias de alumnos. */
@Service
public class AsistenciaJustificacionService {

  private final AsistenciaEstudianteRepository asistencias;
  private final AccesoAsistenciaService acceso;
  private final EvidenciaStorageService storage;

  public AsistenciaJustificacionService(
      AsistenciaEstudianteRepository asistencias,
      AccesoAsistenciaService acceso,
      EvidenciaStorageService storage) {
    this.asistencias = asistencias;
    this.acceso = acceso;
    this.storage = storage;
  }

  @Transactional
  public AsistenciaJustificacionResponse justificar(
      Integer asistenciaId, String motivo, MultipartFile documento, String username) {
    AsistenciaEstudiante asistencia = buscar(asistenciaId);
    acceso.exigirAccesoJustificacion(asistencia.getEstudiante().getId(), username);
    if (asistencia.getEstadoAsistencia() != EstadoAsistencia.AUSENTE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Solo se puede justificar una inasistencia registrada");
    }
    if (motivo == null || motivo.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
    }
    if (documento == null || documento.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunte el documento de sustento");
    }

    EvidenciaStorageService.ArchivoGuardado guardado = storage.guardar(documento);
    asistencia.justificar(motivo, guardado.clave());
    asistencia.setDocumentoJustificacion(
        guardado.clave(), guardado.nombre(), guardado.tipo(), guardado.tamanio());
    return respuesta(asistencia);
  }

  @Transactional(readOnly = true)
  public DocumentoJustificacion descargar(Integer asistenciaId, String username) {
    AsistenciaEstudiante asistencia = buscar(asistenciaId);
    acceso.exigirAccesoHistorial(asistencia.getEstudiante().getId(), username);
    if (asistencia.getArchivoJustificacion() == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay documento de justificación");
    }
    return new DocumentoJustificacion(
        storage.cargar(asistencia.getArchivoJustificacion()),
        asistencia.getDocumentoJustificacionNombre(),
        asistencia.getDocumentoJustificacionTipo());
  }

  private AsistenciaEstudiante buscar(Integer asistenciaId) {
    return asistencias
        .findByIdAndActiveTrue(asistenciaId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe la asistencia del estudiante"));
  }

  private AsistenciaJustificacionResponse respuesta(AsistenciaEstudiante asistencia) {
    return new AsistenciaJustificacionResponse(
        asistencia.getId(),
        asistencia.getEstudiante().getId(),
        asistencia.getEstadoAsistencia(),
        asistencia.getMotivoJustificacion(),
        asistencia.getDocumentoJustificacionNombre(),
        asistencia.getDocumentoJustificacionTipo(),
        asistencia.getDocumentoJustificacionTamanio(),
        "/api/v1/asistencias-estudiantes/" + asistencia.getId() + "/justificacion/documento");
  }

  public record DocumentoJustificacion(Resource recurso, String nombre, String tipo) {}
}
