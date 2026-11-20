package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.ComentarioLibretaRequest;
import pe.edu.pucp.colegio.common.dto.ComentarioLibretaResponse;
import pe.edu.pucp.colegio.common.model.ComentarioLibreta;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.ComentarioLibretaRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Comentario cualitativo único asociado a la libreta del alumno en un período. */
@Service
public class ComentarioLibretaService {

  private final ComentarioLibretaRepository comentarios;
  private final MatriculaRepository matriculas;
  private final CuentaUsuarioRepository cuentas;
  private final SesionHorarioRepository asignaciones;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final Clock clock;

  public ComentarioLibretaService(
      ComentarioLibretaRepository comentarios,
      MatriculaRepository matriculas,
      CuentaUsuarioRepository cuentas,
      SesionHorarioRepository asignaciones,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      Clock clock) {
    this.comentarios = comentarios;
    this.matriculas = matriculas;
    this.cuentas = cuentas;
    this.asignaciones = asignaciones;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ComentarioLibretaResponse obtener(
      Integer estudianteId, Integer periodoId, String username) {
    var matricula = matriculaConfirmada(estudianteId, periodoId);
    CuentaUsuario cuenta = cuenta(username);
    exigirLectura(cuenta, matricula);
    return comentarios
        .findByMatriculaIdAndActiveTrue(matricula.getId())
        .map(this::respuesta)
        .orElse(new ComentarioLibretaResponse(estudianteId, periodoId, null, null, null));
  }

  @Transactional
  public ComentarioLibretaResponse guardar(
      Integer estudianteId, Integer periodoId, ComentarioLibretaRequest request, String username) {
    var matricula = matriculaConfirmada(estudianteId, periodoId);
    CuentaUsuario cuenta = cuenta(username);
    exigirEdicion(cuenta, matricula);
    ComentarioLibreta comentario =
        comentarios
            .findByMatriculaIdAndActiveTrue(matricula.getId())
            .orElseGet(ComentarioLibreta::new);
    comentario.setMatricula(matricula);
    comentario.setComentario(request.comentario().trim());
    comentario.setAutorCuentaId(cuenta.getId());
    comentario.setAutorNombre(
        cuenta.getPersona() == null
            ? cuenta.getUsername()
            : cuenta.getPersona().getNombreCompleto());
    comentario.setActualizadoEn(LocalDateTime.now(clock));
    return respuesta(comentarios.save(comentario));
  }

  private pe.edu.pucp.colegio.common.model.Matricula matriculaConfirmada(
      Integer estudianteId, Integer periodoId) {
    return matriculas
        .findByEstudianteIdAndPeriodoIdAndEstado(
            estudianteId, periodoId, EstadoMatricula.CONFIRMADA)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No hay matrícula confirmada para este período"));
  }

  private CuentaUsuario cuenta(String username) {
    return cuentas
        .findByUsername(username)
        .filter(actual -> Boolean.TRUE.equals(actual.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no válida"));
  }

  private void exigirLectura(
      CuentaUsuario cuenta, pe.edu.pucp.colegio.common.model.Matricula matricula) {
    if (cuenta.getRol() == Rol.ADMINISTRADOR) return;
    if (cuenta.getRol() == Rol.ALUMNO
        && cuenta.getPersona() != null
        && cuenta.getPersona().getId().equals(matricula.getEstudiante().getPersona().getId()))
      return;
    if (cuenta.getRol() == Rol.APODERADO && vinculado(cuenta, matricula)) return;
    if (docenteAsignado(cuenta, matricula)) return;
    throw prohibido();
  }

  private void exigirEdicion(
      CuentaUsuario cuenta, pe.edu.pucp.colegio.common.model.Matricula matricula) {
    if (cuenta.getRol() == Rol.ADMINISTRADOR || docenteAsignado(cuenta, matricula)) return;
    throw prohibido();
  }

  private boolean docenteAsignado(
      CuentaUsuario cuenta, pe.edu.pucp.colegio.common.model.Matricula matricula) {
    if (cuenta.getRol() != Rol.DOCENTE || cuenta.getPersona() == null) return false;
    var periodo = matricula.getPeriodo();
    return !asignaciones
        .buscarAsignacionesDocente(
            cuenta.getPersona().getId(),
            periodo.getSeccion().getId(),
            periodo.getFechaInicio(),
            periodo.getFechaFin())
        .isEmpty();
  }

  private boolean vinculado(
      CuentaUsuario cuenta, pe.edu.pucp.colegio.common.model.Matricula matricula) {
    if (cuenta.getPersona() == null) return false;
    var apoderado =
        apoderados.findByPersonaIdAndActiveTrue(cuenta.getPersona().getId()).orElse(null);
    return apoderado != null
        && vinculos.existsByEstudianteIdAndApoderadoIdAndActiveTrue(
            matricula.getEstudiante().getId(), apoderado.getId());
  }

  private static ResponseStatusException prohibido() {
    return new ResponseStatusException(
        HttpStatus.FORBIDDEN, "No tiene permiso para consultar o editar esta libreta");
  }

  private ComentarioLibretaResponse respuesta(ComentarioLibreta comentario) {
    return new ComentarioLibretaResponse(
        comentario.getMatricula().getEstudiante().getId(),
        comentario.getMatricula().getPeriodo().getId(),
        comentario.getComentario(),
        comentario.getAutorNombre(),
        comentario.getActualizadoEn());
  }
}
