package pe.edu.pucp.colegio.common.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.DocenteResponse;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/**
 * Relaciona cada cuenta con rol DOCENTE con su registro de docente. Ese registro se crea la primera
 * vez que se necesita, porque el alta de usuarios solo crea la cuenta y la persona.
 */
@Service
public class DocenteService {

  private final DocenteRepository docentes;
  private final CuentaUsuarioRepository cuentas;

  public DocenteService(DocenteRepository docentes, CuentaUsuarioRepository cuentas) {
    this.docentes = docentes;
    this.cuentas = cuentas;
  }

  @Transactional
  public List<DocenteResponse> listar() {
    return cuentas.findAllByRolAndActiveTrue(Rol.DOCENTE).stream()
        .filter(cuenta -> cuenta.getPersona() != null)
        .map(cuenta -> respuesta(cuenta, obtenerOCrear(cuenta.getPersona())))
        .filter(Objects::nonNull)
        .sorted(
            Comparator.comparing(DocenteResponse::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Transactional
  public Docente obtenerDeCuenta(String username) {
    CuentaUsuario cuenta =
        cuentas
            .findByUsername(username)
            .filter(encontrada -> Boolean.TRUE.equals(encontrada.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no válida"));
    if (cuenta.getRol() != Rol.DOCENTE || cuenta.getPersona() == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "La cuenta no corresponde a un docente");
    }
    Docente docente = obtenerOCrear(cuenta.getPersona());
    if (docente == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El docente está inactivo");
    }
    return docente;
  }

  /** Devuelve nulo si el docente existe pero fue desactivado. */
  private Docente obtenerOCrear(Persona persona) {
    Docente existente = docentes.findByPersonaId(persona.getId()).orElse(null);
    if (existente != null) {
      return Boolean.TRUE.equals(existente.getActive()) ? existente : null;
    }
    Docente nuevo = new Docente();
    nuevo.setPersona(persona);
    return docentes.save(nuevo);
  }

  private DocenteResponse respuesta(CuentaUsuario cuenta, Docente docente) {
    if (docente == null) {
      return null;
    }
    Persona persona = cuenta.getPersona();
    return new DocenteResponse(
        docente.getId(),
        cuenta.getId(),
        cuenta.getUsername(),
        persona.getNombreCompleto(),
        persona.getDni());
  }
}
