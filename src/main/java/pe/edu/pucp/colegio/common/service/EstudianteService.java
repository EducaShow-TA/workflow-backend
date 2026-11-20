package pe.edu.pucp.colegio.common.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.dto.CuentaTemporalResponse;
import pe.edu.pucp.colegio.common.dto.EstudianteCreateRequest;
import pe.edu.pucp.colegio.common.dto.EstudianteListadoResponse;
import pe.edu.pucp.colegio.common.dto.EstudianteResponse;
import pe.edu.pucp.colegio.common.dto.MatriculaResponse;
import pe.edu.pucp.colegio.common.dto.VerificacionApoderadoRequest;
import pe.edu.pucp.colegio.common.dto.VerificacionApoderadoResponse;
import pe.edu.pucp.colegio.common.dto.VincularApoderadoRequest;
import pe.edu.pucp.colegio.common.dto.VinculoFamiliarResponse;
import pe.edu.pucp.colegio.common.dto.VinculoFamiliarUpdateRequest;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Parentesco;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.common.validation.UsernamePolicy;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Gestiona el registro, consulta y asignación de perfiles de estudiante. */
@Service
public class EstudianteService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CARACTERES =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*?";
  private static final String[] REQUERIDOS = {
    "ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!@#$%&*?"
  };

  private final PersonaRepository personas;
  private final EstudianteRepository estudiantes;
  private final CuentaUsuarioRepository cuentas;
  private final PasswordEncoder passwordEncoder;
  private final pe.edu.pucp.colegio.common.repository.MatriculaRepository matriculas;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final RegistroAuditoriaRepository auditorias;

  public EstudianteService(
      PersonaRepository personas,
      EstudianteRepository estudiantes,
      CuentaUsuarioRepository cuentas,
      PasswordEncoder passwordEncoder,
      pe.edu.pucp.colegio.common.repository.MatriculaRepository matriculas,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      RegistroAuditoriaRepository auditorias) {
    this.personas = personas;
    this.estudiantes = estudiantes;
    this.cuentas = cuentas;
    this.passwordEncoder = passwordEncoder;
    this.matriculas = matriculas;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.auditorias = auditorias;
  }

  @Transactional(readOnly = true)
  public Page<EstudianteListadoResponse> listar(
      String nombre, String dni, Integer gradoId, Integer seccionId, int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación inválida");
    }
    Page<Estudiante> pagina =
        estudiantes.buscar(
            nombre == null || nombre.isBlank() ? null : nombre.trim(),
            dni == null || dni.isBlank() ? null : dni.trim(),
            gradoId,
            seccionId,
            PageRequest.of(page, size, Sort.by("id").ascending()));
    var ids = pagina.getContent().stream().map(Estudiante::getId).toList();
    var porEstudiante = new HashMap<Integer, List<MatriculaResponse>>();
    if (!ids.isEmpty()) {
      for (Matricula matricula : matriculas.findAllByEstudianteIdIn(ids)) {
        porEstudiante
            .computeIfAbsent(matricula.getEstudiante().getId(), ignored -> new ArrayList<>())
            .add(
                new MatriculaResponse(
                    matricula.getId(),
                    matricula.getEstudiante().getId(),
                    matricula.getPeriodo().getId(),
                    matricula.getFechaMatricula(),
                    matricula.getEstado()));
      }
    }
    return pagina.map(
        estudiante -> {
          Persona persona = estudiante.getPersona();
          return new EstudianteListadoResponse(
              estudiante.getId(),
              persona.getId(),
              persona.getDni(),
              persona.getNombre(),
              persona.getApellidoPaterno(),
              persona.getApellidoMaterno(),
              persona.getCorreo(),
              porEstudiante.getOrDefault(estudiante.getId(), List.of()));
        });
  }

  @Transactional
  public EstudianteResponse crear(EstudianteCreateRequest request) {
    validarDocumento(request.tipoDocumento(), request.numeroDocumento());
    long principales =
        request.apoderados().stream()
            .filter(EstudianteCreateRequest.ApoderadoCreateRequest::esPrincipal)
            .count();
    if (principales != 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Debe indicar exactamente un apoderado principal");
    }
    long documentosApoderadoUnicos =
        request.apoderados().stream()
            .map(EstudianteCreateRequest.ApoderadoCreateRequest::numeroDocumento)
            .distinct()
            .count();
    if (documentosApoderadoUnicos != request.apoderados().size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No se puede repetir el documento de un apoderado");
    }
    if (request.apoderados().stream()
        .anyMatch(apoderado -> apoderado.numeroDocumento().equals(request.numeroDocumento()))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El estudiante no puede registrarse como su propio apoderado");
    }
    if (personas.existsByDni(request.numeroDocumento())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe una persona con ese documento");
    }
    if (cuentas.existsByUsernameIgnoreCase(request.username().trim())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe una cuenta con ese nombre de usuario");
    }
    if (!cuentas.findAllByPersonaCorreoIgnoreCase(request.username().trim()).isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El usuario coincide con el correo de otra cuenta");
    }
    if (request.correoElectronico() != null
        && !request.correoElectronico().isBlank()
        && !cuentas
            .findAllByPersonaCorreoIgnoreCase(request.correoElectronico().trim())
            .isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El correo ya está asociado a otra cuenta");
    }
    if (request.correoElectronico() != null
        && !request.correoElectronico().isBlank()
        && cuentas.findByUsernameIgnoreCase(request.correoElectronico().trim()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El correo coincide con el usuario de otra cuenta");
    }

    Persona persona =
        new Persona(
            request.numeroDocumento(),
            request.nombres(),
            request.apellidoPaterno(),
            request.apellidoMaterno());
    persona.setTipoDocumento(request.tipoDocumento());
    persona.setFechaNacimiento(request.fechaNacimiento());
    persona.setTelefono(request.celular());
    persona.setCorreo(request.correoElectronico());
    persona.setNacionalidad(request.nacionalidad());
    persona.setDireccion(request.direccion());
    if (request.genero() != null && !request.genero().isBlank()) {
      persona.setGenero(request.genero());
    }
    persona = personas.save(persona);

    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    estudiante = estudiantes.save(estudiante);

    CuentaUsuario cuenta =
        new CuentaUsuario(
            request.username(), passwordEncoder.encode(request.password()), Rol.ALUMNO, persona);
    cuentas.save(cuenta);

    List<CuentaTemporalResponse> cuentasApoderados = new ArrayList<>();
    for (EstudianteCreateRequest.ApoderadoCreateRequest datos : request.apoderados()) {
      validarDocumento(datos.tipoDocumento(), datos.numeroDocumento());
      Persona personaApoderado = personas.findByDni(datos.numeroDocumento()).orElse(null);
      if (personaApoderado == null) {
        personaApoderado =
            new Persona(
                datos.numeroDocumento(),
                datos.nombres(),
                datos.apellidoPaterno(),
                datos.apellidoMaterno());
        personaApoderado.setTipoDocumento(datos.tipoDocumento());
        personaApoderado.setTelefono(datos.celular());
        personaApoderado.setCorreo(datos.correoElectronico());
        personaApoderado = personas.save(personaApoderado);
      } else if (!personaApoderado.getTipoDocumento().equals(datos.tipoDocumento())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "El tipo de documento del apoderado no coincide");
      }
      Apoderado apoderado = apoderados.findByPersonaDni(datos.numeroDocumento()).orElse(null);
      if (apoderado == null) {
        Apoderado nuevo = new Apoderado();
        nuevo.setPersona(personaApoderado);
        apoderado = apoderados.save(nuevo);
      }
      CuentaTemporalResponse credencial = asegurarCuentaApoderado(personaApoderado);
      if (credencial != null) {
        cuentasApoderados.add(credencial);
      }
      VinculoFamiliar vinculo = new VinculoFamiliar();
      vinculo.setEstudiante(estudiante);
      vinculo.setApoderado(apoderado);
      vinculo.setParentesco(Parentesco.valueOf(datos.parentesco()));
      vinculo.setEsPrincipal(datos.esPrincipal());
      vinculos.save(vinculo);
    }

    return new EstudianteResponse(
        estudiante.getId(),
        persona.getId(),
        cuenta.getUsername(),
        cuenta.getRol().name(),
        persona.getDni(),
        persona.getNombreCompleto(),
        List.copyOf(cuentasApoderados));
  }

  @Transactional(readOnly = true)
  public List<VinculoFamiliarResponse> listarApoderados(Integer estudianteId) {
    Estudiante estudiante = obtenerEstudiante(estudianteId);
    return vinculos.findAllByEstudianteId(estudiante.getId()).stream()
        .filter(
            v ->
                Boolean.TRUE.equals(v.getActive())
                    && Boolean.TRUE.equals(v.getApoderado().getActive()))
        .map(this::respuestaVinculo)
        .toList();
  }

  @Transactional
  public VerificacionApoderadoResponse verificarApoderado(
      Integer estudianteId, VerificacionApoderadoRequest request, String usernameResponsable) {
    validarDocumento(request.tipoDocumento(), request.numeroDocumento());
    Estudiante estudiante = obtenerEstudiante(estudianteId);
    CuentaUsuario responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    auditorias.save(
        new RegistroAuditoria(
            responsable == null ? null : responsable.getId(),
            responsable == null ? null : responsable.getRol().name(),
            AccionAuditoria.CONSULTAR_DOCUMENTO,
            "Verificación de "
                + request.tipoDocumento()
                + " "
                + request.numeroDocumento()
                + " para vincular apoderado al estudiante "
                + estudiante.getId()));

    if (estudiante.getPersona().getDni().equals(request.numeroDocumento())) {
      return VerificacionApoderadoResponse.rechazada(
          true, "El estudiante no puede registrarse como su propio apoderado");
    }
    Persona persona = personas.findByDni(request.numeroDocumento()).orElse(null);
    if (persona == null) {
      return VerificacionApoderadoResponse.nueva();
    }
    if (!request.tipoDocumento().equals(persona.getTipoDocumento())) {
      return VerificacionApoderadoResponse.rechazada(
          true, "El tipo de documento no coincide con el registrado");
    }
    Optional<CuentaUsuario> cuenta = cuentas.findByPersonaDni(persona.getDni());
    if (cuenta.isPresent()
        && (cuenta.get().getRol() != Rol.APODERADO
            || !Boolean.TRUE.equals(cuenta.get().getActive()))) {
      return VerificacionApoderadoResponse.rechazada(
          true, "La persona ya tiene una cuenta con otro rol o inactiva");
    }
    boolean yaVinculado =
        apoderados
            .findByPersonaDni(persona.getDni())
            .flatMap(a -> vinculos.findByEstudianteIdAndApoderadoId(estudianteId, a.getId()))
            .filter(v -> Boolean.TRUE.equals(v.getActive()))
            .isPresent();
    return new VerificacionApoderadoResponse(
        true,
        !yaVinculado,
        yaVinculado ? "El apoderado ya está vinculado al estudiante" : null,
        persona.getNombreCompleto(),
        enmascararCelular(persona.getTelefono()),
        enmascararCorreo(persona.getCorreo()),
        cuenta.isPresent());
  }

  @Transactional
  public VinculoFamiliarResponse agregarApoderado(
      Integer estudianteId, VincularApoderadoRequest datos) {
    validarDocumento(datos.tipoDocumento(), datos.numeroDocumento());
    Estudiante estudiante = obtenerEstudiante(estudianteId);
    if (estudiante.getPersona().getDni().equals(datos.numeroDocumento())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El estudiante no puede registrarse como su propio apoderado");
    }
    Persona persona = personas.findByDni(datos.numeroDocumento()).orElse(null);
    if (persona == null) {
      exigirDatoNuevo(datos.nombres(), "los nombres son obligatorios");
      exigirDatoNuevo(datos.apellidoPaterno(), "el apellido paterno es obligatorio");
      exigirDatoNuevo(datos.apellidoMaterno(), "el apellido materno es obligatorio");
      exigirDatoNuevo(datos.celular(), "el celular es obligatorio");
      persona =
          new Persona(
              datos.numeroDocumento(),
              datos.nombres().trim(),
              datos.apellidoPaterno().trim(),
              datos.apellidoMaterno().trim());
      persona.setTipoDocumento(datos.tipoDocumento());
      persona.setTelefono(datos.celular());
      persona.setCorreo(datos.correoElectronico());
      persona = personas.save(persona);
    } else if (!persona.getTipoDocumento().equals(datos.tipoDocumento())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El tipo de documento del apoderado no coincide");
    }
    Apoderado apoderado = apoderados.findByPersonaDni(datos.numeroDocumento()).orElse(null);
    if (apoderado == null) {
      apoderado = new Apoderado();
      apoderado.setPersona(persona);
      apoderado = apoderados.save(apoderado);
    } else if (!Boolean.TRUE.equals(apoderado.getActive())) {
      apoderado.setActive(true);
      apoderado = apoderados.save(apoderado);
    }
    CuentaTemporalResponse credencial = asegurarCuentaApoderado(persona);
    VinculoFamiliar vinculo =
        vinculos.findByEstudianteIdAndApoderadoId(estudianteId, apoderado.getId()).orElse(null);
    if (vinculo == null) {
      vinculo = new VinculoFamiliar();
      vinculo.setEstudiante(estudiante);
      vinculo.setApoderado(apoderado);
    } else if (Boolean.TRUE.equals(vinculo.getActive())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El apoderado ya está vinculado al estudiante");
    }
    vinculo.setActive(true);
    vinculo.setParentesco(Parentesco.valueOf(datos.parentesco()));
    vinculo.setEsPrincipal(datos.esPrincipal());
    if (datos.esPrincipal()) {
      quitarPrincipalAOtros(estudianteId, apoderado.getId());
    }
    return respuestaVinculo(
        vinculos.save(vinculo), credencial == null ? null : credencial.contrasenhaTemporal());
  }

  private void exigirDatoNuevo(String valor, String mensaje) {
    if (valor == null || valor.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Para un apoderado nuevo, " + mensaje);
    }
  }

  private static String enmascararCelular(String celular) {
    if (celular == null || celular.isBlank()) {
      return null;
    }
    String limpio = celular.trim();
    if (limpio.length() <= 5) {
      return "\u2022".repeat(limpio.length());
    }
    return limpio.charAt(0)
        + "\u2022".repeat(limpio.length() - 5)
        + limpio.substring(limpio.length() - 4);
  }

  private static String enmascararCorreo(String correo) {
    if (correo == null || correo.isBlank()) {
      return null;
    }
    int arroba = correo.indexOf('@');
    if (arroba <= 0) {
      return "\u2022\u2022\u2022";
    }
    return correo.charAt(0) + "\u2022\u2022\u2022" + correo.substring(arroba);
  }

  @Transactional
  public VinculoFamiliarResponse actualizarVinculo(
      Integer estudianteId, Integer apoderadoId, VinculoFamiliarUpdateRequest request) {
    obtenerEstudiante(estudianteId);
    VinculoFamiliar vinculo =
        vinculos
            .findByEstudianteIdAndApoderadoId(estudianteId, apoderadoId)
            .filter(v -> Boolean.TRUE.equals(v.getActive()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el vínculo familiar indicado"));
    if (vinculo.isEsPrincipal() && !request.esPrincipal()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "El estudiante debe tener un apoderado principal: designa a otro antes de quitarle esa"
              + " condición");
    }
    vinculo.setParentesco(Parentesco.valueOf(request.parentesco()));
    vinculo.setEsPrincipal(request.esPrincipal());
    Persona personaApoderado = vinculo.getApoderado().getPersona();
    if (request.nombres() != null) personaApoderado.setNombre(request.nombres());
    if (request.apellidoPaterno() != null)
      personaApoderado.setApellidoPaterno(request.apellidoPaterno());
    if (request.apellidoMaterno() != null)
      personaApoderado.setApellidoMaterno(request.apellidoMaterno());
    if (request.celular() != null) personaApoderado.setTelefono(request.celular());
    if (request.correoElectronico() != null)
      personaApoderado.setCorreo(request.correoElectronico());
    if (request.esPrincipal()) {
      quitarPrincipalAOtros(estudianteId, apoderadoId);
    }
    return respuestaVinculo(vinculos.save(vinculo));
  }

  private Estudiante obtenerEstudiante(Integer estudianteId) {
    return estudiantes
        .findById(estudianteId)
        .filter(e -> Boolean.TRUE.equals(e.getActive()))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe el estudiante indicado"));
  }

  private void quitarPrincipalAOtros(Integer estudianteId, Integer apoderadoId) {
    for (VinculoFamiliar otro : vinculos.findAllByEstudianteId(estudianteId)) {
      if (Boolean.TRUE.equals(otro.getActive())
          && !otro.getApoderado().getId().equals(apoderadoId)
          && otro.isEsPrincipal()) {
        otro.setEsPrincipal(false);
        vinculos.save(otro);
      }
    }
  }

  private VinculoFamiliarResponse respuestaVinculo(VinculoFamiliar vinculo) {
    return respuestaVinculo(vinculo, null);
  }

  private VinculoFamiliarResponse respuestaVinculo(
      VinculoFamiliar vinculo, String contrasenhaTemporal) {
    Persona persona = vinculo.getApoderado().getPersona();
    String username =
        cuentas.findByPersonaDni(persona.getDni()).map(CuentaUsuario::getUsername).orElse(null);
    return new VinculoFamiliarResponse(
        vinculo.getApoderado().getId(),
        persona.getTipoDocumento(),
        persona.getDni(),
        persona.getNombre(),
        persona.getApellidoPaterno(),
        persona.getApellidoMaterno(),
        persona.getTelefono(),
        persona.getCorreo(),
        vinculo.getParentesco().name(),
        vinculo.isEsPrincipal(),
        username,
        contrasenhaTemporal);
  }

  private CuentaTemporalResponse asegurarCuentaApoderado(Persona persona) {
    CuentaUsuario existente = cuentas.findByPersonaDni(persona.getDni()).orElse(null);
    if (existente != null) {
      if (existente.getRol() != Rol.APODERADO || !Boolean.TRUE.equals(existente.getActive())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "La persona ya tiene una cuenta con otro rol o inactiva");
      }
      return null;
    }
    String username = UsernamePolicy.generarDesdeDocumento(persona.getDni());
    if (cuentas.existsByUsernameIgnoreCase(username)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "No se pudo generar un usuario único para el apoderado");
    }
    String password = generarContrasenhaTemporal();
    cuentas.save(
        new CuentaUsuario(username, passwordEncoder.encode(password), Rol.APODERADO, persona));
    return new CuentaTemporalResponse(persona.getDni(), username, password);
  }

  private String generarContrasenhaTemporal() {
    char[] clave = new char[12];
    for (int i = 0; i < REQUERIDOS.length; i++) {
      String grupo = REQUERIDOS[i];
      clave[i] = grupo.charAt(RANDOM.nextInt(grupo.length()));
    }
    for (int i = 4; i < clave.length; i++) {
      clave[i] = CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length()));
    }
    for (int i = clave.length - 1; i > 0; i--) {
      int j = RANDOM.nextInt(i + 1);
      char temporal = clave[i];
      clave[i] = clave[j];
      clave[j] = temporal;
    }
    return new String(clave);
  }

  private void validarDocumento(String tipo, String numero) {
    boolean valido =
        "DNI".equals(tipo)
            ? numero.matches("\\d{8}")
            : "PASAPORTE".equals(tipo) && numero.matches("[A-Z0-9]{6,12}");
    if (!valido) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El número de documento no cumple el formato indicado");
    }
  }

  @Transactional
  public EstudianteResponse asignarCuentaExistente(
      Integer cuentaUsuarioId, String usernameResponsable) {
    CuentaUsuario cuenta =
        cuentas
            .findById(cuentaUsuarioId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la cuenta de usuario indicada"));

    if (cuenta.getRol() == Rol.ADMINISTRADOR) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "No se puede asignar el perfil de estudiante a una cuenta de administrador");
    }

    Persona persona = cuenta.getPersona();
    if (persona == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "La cuenta debe estar vinculada a una Persona para asignarle el perfil de estudiante");
    }
    if (estudiantes.existsByPersonaId(persona.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La Persona ya tiene un perfil de estudiante");
    }

    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    estudiante = estudiantes.save(estudiante);

    Rol rolAnterior = cuenta.getRol();
    cuenta.setRol(Rol.ALUMNO);
    cuentas.save(cuenta);
    CuentaUsuario responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    auditorias.save(
        new RegistroAuditoria(
            responsable == null ? null : responsable.getId(),
            responsable == null ? null : responsable.getRol().name(),
            AccionAuditoria.ACTUALIZAR,
            "Cambio de rol de cuenta " + cuenta.getUsername() + ": " + rolAnterior + " -> ALUMNO"));

    return new EstudianteResponse(
        estudiante.getId(),
        persona.getId(),
        cuenta.getUsername(),
        cuenta.getRol().name(),
        persona.getDni(),
        persona.getNombreCompleto());
  }
}
