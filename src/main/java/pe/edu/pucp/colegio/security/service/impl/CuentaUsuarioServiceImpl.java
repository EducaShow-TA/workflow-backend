package pe.edu.pucp.colegio.security.service.impl;

import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.dto.CuentaTemporalResponse;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.validation.UsernamePolicy;
import pe.edu.pucp.colegio.security.dto.ActualizarCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CambiarEstadoCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CambiarRolCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CrearCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CrearDocenteRequestDTO;
import pe.edu.pucp.colegio.security.dto.CuentaResponseDTO;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.service.ContrasenhaTemporal;
import pe.edu.pucp.colegio.security.service.CuentaUsuarioService;

/** Gestiona la creación, consulta, actualización y estado de las cuentas. */
@Service
public class CuentaUsuarioServiceImpl implements CuentaUsuarioService {

  private static final Logger log = LoggerFactory.getLogger(CuentaUsuarioServiceImpl.class);

  private final CuentaUsuarioRepository cuentas;
  private final PersonaRepository personas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final DocenteRepository docentes;
  private final RegistroAuditoriaRepository auditorias;
  private final PasswordEncoder encoder;

  public CuentaUsuarioServiceImpl(
      CuentaUsuarioRepository cuentas,
      PersonaRepository personas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      DocenteRepository docentes,
      RegistroAuditoriaRepository auditorias,
      PasswordEncoder encoder) {
    this.cuentas = cuentas;
    this.personas = personas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.docentes = docentes;
    this.auditorias = auditorias;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public CuentaResponseDTO crear(CrearCuentaRequestDTO request, String usernameResponsable) {
    if (cuentas.existsByUsernameIgnoreCase(request.username().trim())) {
      throw new ConflictException("Ya existe una cuenta con ese usuario");
    }
    if (!cuentas.findAllByPersonaCorreoIgnoreCase(request.username().trim()).isEmpty()) {
      throw new ConflictException("El usuario coincide con el correo de contacto de otra cuenta");
    }
    if (!cuentas.findAllByPersonaCorreoIgnoreCase(request.correo().trim()).isEmpty()) {
      throw new ConflictException("El correo ya está asociado a otra cuenta");
    }
    if (cuentas.findByUsernameIgnoreCase(request.correo().trim()).isPresent()) {
      throw new ConflictException("El correo coincide con el usuario de otra cuenta");
    }
    if (personas.existsByDni(request.dni())) {
      throw new ConflictException("Ya existe una persona con ese DNI");
    }
    Persona persona =
        new Persona(
            request.dni(), request.nombre(), request.apellidoPaterno(), request.apellidoMaterno());
    persona.setCorreo(request.correo().trim());
    persona = personas.save(persona);
    CuentaUsuario cuenta =
        cuentas.save(
            new CuentaUsuario(
                request.username(), encoder.encode(request.password()), request.rol(), persona));
    if (request.rol() == pe.edu.pucp.colegio.security.model.Rol.ALUMNO) {
      Estudiante estudiante = new Estudiante();
      estudiante.setPersona(persona);
      estudiantes.save(estudiante);
    } else if (request.rol() == pe.edu.pucp.colegio.security.model.Rol.APODERADO) {
      Apoderado apoderado = new Apoderado();
      apoderado.setPersona(persona);
      apoderados.save(apoderado);
    } else if (request.rol() == Rol.DOCENTE) {
      Docente docente = new Docente();
      docente.setPersona(persona);
      docentes.save(docente);
    }
    registrarAuditoriaCreacion(cuenta, usernameResponsable);
    return aDto(cuenta);
  }

  @Override
  @Transactional
  public CuentaTemporalResponse crearDocente(
      CrearDocenteRequestDTO request, String usernameResponsable) {
    String dni = request.dni().trim();
    String correo = request.correo().trim().toLowerCase(Locale.ROOT);
    String username = UsernamePolicy.generarDesdeDocumento(dni);
    if (cuentas.existsByUsernameIgnoreCase(username)) {
      throw new ConflictException("No se pudo generar un usuario único para el docente");
    }
    if (!cuentas.findAllByPersonaCorreoIgnoreCase(username).isEmpty()) {
      throw new ConflictException("El usuario generado coincide con el correo de otra cuenta");
    }
    if (!cuentas.findAllByPersonaCorreoIgnoreCase(correo).isEmpty()) {
      throw new ConflictException("El correo ya está asociado a otra cuenta");
    }
    if (cuentas.findByUsernameIgnoreCase(correo).isPresent()) {
      throw new ConflictException("El correo coincide con el usuario de otra cuenta");
    }
    if (personas.existsByDni(dni)) {
      throw new ConflictException("Ya existe una persona con ese DNI");
    }

    Persona persona =
        new Persona(
            dni,
            request.nombre().trim(),
            request.apellidoPaterno().trim(),
            request.apellidoMaterno().trim());
    persona.setCorreo(correo);
    persona = personas.save(persona);

    String contrasenhaTemporal = ContrasenhaTemporal.generar();
    CuentaUsuario cuenta =
        cuentas.save(
            new CuentaUsuario(username, encoder.encode(contrasenhaTemporal), Rol.DOCENTE, persona));
    Docente docente = new Docente();
    docente.setPersona(persona);
    docentes.save(docente);

    registrarAuditoriaCreacion(cuenta, usernameResponsable);
    return new CuentaTemporalResponse(dni, username, contrasenhaTemporal);
  }

  private void registrarAuditoriaCreacion(CuentaUsuario cuenta, String usernameResponsable) {
    var responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    auditorias.save(
        new RegistroAuditoria(
            responsable == null ? null : responsable.getId(),
            responsable == null ? null : responsable.getRol().name(),
            AccionAuditoria.CREAR,
            "Alta de cuenta " + cuenta.getUsername() + " con rol " + cuenta.getRol()));
    log.info(
        "event=cuenta_creada id={} rol={} por={}",
        cuenta.getId(),
        cuenta.getRol(),
        usernameResponsable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CuentaResponseDTO> listar() {
    return cuentas.findAll().stream().map(this::aDto).toList();
  }

  @Override
  @Transactional
  public CuentaResponseDTO actualizar(
      Integer id, ActualizarCuentaRequestDTO request, String usernameResponsable) {
    CuentaUsuario cuenta =
        cuentas
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    String username = request.username().trim();
    if (cuentas.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
      throw new ConflictException("Ya existe una cuenta con ese usuario");
    }
    if (cuentas.findAllByPersonaCorreoIgnoreCase(username).stream()
        .anyMatch(otra -> !otra.getId().equals(id))) {
      throw new ConflictException("El usuario coincide con el correo de contacto de otra cuenta");
    }
    if (cuentas.findAllByPersonaCorreoIgnoreCase(request.correo().trim()).stream()
        .anyMatch(otra -> !otra.getId().equals(id))) {
      throw new ConflictException("El correo ya está asociado a otra cuenta");
    }
    if (cuentas
        .findByUsernameIgnoreCase(request.correo().trim())
        .filter(otra -> !otra.getId().equals(id))
        .isPresent()) {
      throw new ConflictException("El correo coincide con el usuario de otra cuenta");
    }

    var responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    cuenta.setUsername(username);
    if (cuenta.getPersona() != null) {
      cuenta.getPersona().setNombre(request.nombre().trim());
      cuenta.getPersona().setApellidoPaterno(request.apellidoPaterno().trim());
      cuenta.getPersona().setApellidoMaterno(request.apellidoMaterno().trim());
      cuenta.getPersona().setCorreo(request.correo().trim());
    }
    auditorias.save(
        new RegistroAuditoria(
            responsable == null ? null : responsable.getId(),
            responsable == null ? null : responsable.getRol().name(),
            AccionAuditoria.ACTUALIZAR,
            "Actualización de cuenta " + cuenta.getUsername()));
    log.info("event=cuenta_actualizada id={} por={}", id, usernameResponsable);
    return aDto(cuenta);
  }

  @Override
  @Transactional
  public CuentaResponseDTO cambiarEstado(
      Integer id, CambiarEstadoCuentaRequestDTO request, String usernameResponsable) {
    CuentaUsuario cuenta =
        cuentas
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

    var responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    if (!request.activo()
        && Boolean.TRUE.equals(cuenta.getActive())
        && responsable != null
        && responsable.getId().equals(cuenta.getId())) {
      throw new ConflictException("No se puede desactivar la cuenta del administrador autenticado");
    }

    if (!request.activo().equals(cuenta.getActive())) {
      cuenta.setActive(request.activo());
      actualizarEstadoPerfil(cuenta.getPersona(), cuenta.getRol(), request.activo());
      auditorias.save(
          new RegistroAuditoria(
              responsable == null ? null : responsable.getId(),
              responsable == null ? null : responsable.getRol().name(),
              AccionAuditoria.ACTUALIZAR,
              (request.activo() ? "Activación" : "Desactivación")
                  + " de cuenta "
                  + cuenta.getUsername()));
      log.info(
          "event=cuenta_estado_actualizado id={} activo={} por={}",
          id,
          request.activo(),
          usernameResponsable);
    }
    return aDto(cuenta);
  }

  @Override
  @Transactional
  public CuentaResponseDTO cambiarRol(
      Integer id, CambiarRolCuentaRequestDTO request, String usernameResponsable) {
    CuentaUsuario cuenta =
        cuentas
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    CuentaUsuario responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    if (responsable != null && responsable.getId().equals(id)) {
      throw new ConflictException("No se puede cambiar el rol de la cuenta autenticada");
    }
    if (cuenta.getPersona() == null) {
      throw new ConflictException(
          "La cuenta debe estar vinculada a una Persona para cambiar su rol");
    }
    Rol rolAnterior = cuenta.getRol();
    if (rolAnterior == request.rol()) {
      return aDto(cuenta);
    }
    actualizarEstadoPerfil(cuenta.getPersona(), rolAnterior, false);
    actualizarEstadoPerfil(
        cuenta.getPersona(), request.rol(), Boolean.TRUE.equals(cuenta.getActive()));
    cuenta.setRol(request.rol());
    auditorias.save(
        new RegistroAuditoria(
            responsable == null ? null : responsable.getId(),
            responsable == null ? null : responsable.getRol().name(),
            AccionAuditoria.ACTUALIZAR,
            "Cambio de rol de cuenta "
                + cuenta.getUsername()
                + ": "
                + rolAnterior
                + " -> "
                + request.rol()));
    log.info(
        "event=cuenta_rol_actualizado id={} rol_anterior={} rol_nuevo={} por={}",
        id,
        rolAnterior,
        request.rol(),
        usernameResponsable);
    return aDto(cuenta);
  }

  private void actualizarEstadoPerfil(Persona persona, Rol rol, boolean activo) {
    if (persona == null) {
      return;
    }
    if (rol == Rol.ALUMNO) {
      Estudiante estudiante = estudiantes.findByPersonaDni(persona.getDni()).orElse(null);
      if (estudiante == null && activo) {
        estudiante = new Estudiante();
        estudiante.setPersona(persona);
      }
      if (estudiante != null) {
        estudiante.setActive(activo);
        estudiantes.save(estudiante);
      }
    } else if (rol == Rol.APODERADO) {
      Apoderado apoderado = apoderados.findByPersonaDni(persona.getDni()).orElse(null);
      if (apoderado == null && activo) {
        apoderado = new Apoderado();
        apoderado.setPersona(persona);
      }
      if (apoderado != null) {
        apoderado.setActive(activo);
        apoderados.save(apoderado);
      }
    } else if (rol == Rol.DOCENTE) {
      var docente = docentes.findByPersonaId(persona.getId()).orElse(null);
      if (docente == null && activo) {
        docente = new pe.edu.pucp.colegio.common.model.Docente();
        docente.setPersona(persona);
      }
      if (docente != null) {
        docente.setActive(activo);
        docentes.save(docente);
      }
    }
  }

  private CuentaResponseDTO aDto(CuentaUsuario c) {
    var p = c.getPersona();
    return new CuentaResponseDTO(
        c.getId(),
        c.getUsername(),
        p == null ? null : p.getCorreo(),
        c.getRol().name(),
        p == null ? c.getUsername() : p.getNombreCompleto(),
        p == null ? "" : p.getDni(),
        p == null ? c.getUsername() : p.getNombre(),
        p == null ? "" : p.getApellidoPaterno(),
        p == null ? "" : p.getApellidoMaterno(),
        c.getActive());
  }
}
