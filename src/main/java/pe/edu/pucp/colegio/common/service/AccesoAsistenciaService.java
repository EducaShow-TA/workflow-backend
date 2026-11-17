package pe.edu.pucp.colegio.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Comprueba qué cuentas pueden consultar o justificar la asistencia de un alumno. */
@Service
public class AccesoAsistenciaService {

  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final AsistenciaEstudianteRepository asistencias;
  private final SesionClaseRepository sesiones;

  public AccesoAsistenciaService(
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      AsistenciaEstudianteRepository asistencias,
      SesionClaseRepository sesiones) {
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.asistencias = asistencias;
    this.sesiones = sesiones;
  }

  @Transactional(readOnly = true)
  public Estudiante exigirAccesoHistorial(Integer estudianteId, String username) {
    Estudiante estudiante = buscarEstudiante(estudianteId);
    CuentaUsuario cuenta = buscarCuenta(username);
    if (cuenta.getRol() == Rol.ADMINISTRADOR) {
      return estudiante;
    }
    if (cuenta.getRol() == Rol.DOCENTE && docenteDictaSeccion(cuenta, estudianteId)) {
      return estudiante;
    }
    if (cuenta.getRol() == Rol.ALUMNO && mismaPersona(cuenta, estudiante)) {
      return estudiante;
    }
    if (cuenta.getRol() == Rol.APODERADO && esApoderadoDe(cuenta, estudiante)) {
      return estudiante;
    }
    throw prohibido();
  }

  @Transactional(readOnly = true)
  public Estudiante exigirAccesoJustificacion(Integer estudianteId, String username) {
    Estudiante estudiante = buscarEstudiante(estudianteId);
    CuentaUsuario cuenta = buscarCuenta(username);
    if (cuenta.getRol() == Rol.ALUMNO && mismaPersona(cuenta, estudiante)) {
      return estudiante;
    }
    if (cuenta.getRol() == Rol.APODERADO && esApoderadoDe(cuenta, estudiante)) {
      return estudiante;
    }
    throw prohibido();
  }

  @Transactional(readOnly = true)
  public boolean esDocente(String username) {
    return buscarCuenta(username).getRol() == Rol.DOCENTE;
  }

  @Transactional(readOnly = true)
  public boolean docentePuedeConsultarSesion(String username, SesionClase sesion) {
    CuentaUsuario cuenta = buscarCuenta(username);
    return cuenta.getRol() == Rol.DOCENTE && puedeConsultarSesion(cuenta, sesion);
  }

  private Estudiante buscarEstudiante(Integer estudianteId) {
    return estudiantes
        .findById(estudianteId)
        .filter(estudiante -> Boolean.TRUE.equals(estudiante.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el estudiante"));
  }

  private CuentaUsuario buscarCuenta(String username) {
    return cuentas
        .findByUsername(username)
        .filter(cuenta -> Boolean.TRUE.equals(cuenta.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no válida"));
  }

  private boolean mismaPersona(CuentaUsuario cuenta, Estudiante estudiante) {
    return cuenta.getPersona() != null
        && estudiante.getPersona() != null
        && cuenta.getPersona().getId().equals(estudiante.getPersona().getId());
  }

  private boolean esApoderadoDe(CuentaUsuario cuenta, Estudiante estudiante) {
    if (cuenta.getPersona() == null) {
      return false;
    }
    Apoderado apoderado =
        apoderados.findByPersonaIdAndActiveTrue(cuenta.getPersona().getId()).orElse(null);
    return apoderado != null
        && vinculos.existsByEstudianteIdAndApoderadoIdAndActiveTrue(
            estudiante.getId(), apoderado.getId());
  }

  private boolean docenteDictaSeccion(CuentaUsuario cuenta, Integer estudianteId) {
    if (cuenta.getPersona() == null) {
      return false;
    }
    return asistencias.buscarHistorial(estudianteId, null, null).stream()
        .map(asistencia -> asistencia.getSesionClase())
        .anyMatch(sesion -> puedeConsultarSesion(cuenta, sesion));
  }

  private boolean puedeConsultarSesion(CuentaUsuario cuenta, SesionClase sesion) {
    return cuenta.getPersona() != null
        && (cuenta.getPersona().getId().equals(sesion.getDocente().getPersona().getId())
            || sesiones.docenteTieneClaseEnSeccion(
                cuenta.getPersona().getId(),
                sesion.getSeccion().getId(),
                sesion.getFecha(),
                sesion.getFecha()));
  }

  private ResponseStatusException prohibido() {
    return new ResponseStatusException(
        HttpStatus.FORBIDDEN, "No tiene permiso para consultar o justificar esta asistencia");
  }
}
