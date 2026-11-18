package pe.edu.pucp.colegio.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.EstudianteVinculadoResponse;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Identifica al estudiante que corresponde a la cuenta de alumno autenticada. */
@Service
public class FichaEstudianteService {

  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;

  public FichaEstudianteService(CuentaUsuarioRepository cuentas, EstudianteRepository estudiantes) {
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
  }

  @Transactional(readOnly = true)
  public EstudianteVinculadoResponse obtener(String username) {
    var cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .filter(usuario -> usuario.getRol() == Rol.ALUMNO && usuario.getPersona() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "La cuenta no corresponde a un alumno activo"));
    var estudiante =
        estudiantes
            .findByPersonaIdAndActiveTrue(cuenta.getPersona().getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No existe un registro de estudiante activo"));
    return EstudianteVinculadoResponse.desde(estudiante);
  }
}
