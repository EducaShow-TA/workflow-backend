package pe.edu.pucp.colegio.common.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.EstudianteCreateRequest;
import pe.edu.pucp.colegio.common.dto.EstudianteListadoResponse;
import pe.edu.pucp.colegio.common.dto.EstudianteResponse;
import pe.edu.pucp.colegio.common.dto.MatriculaResponse;
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
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Gestiona el registro, consulta y asignación de perfiles de estudiante. */
@Service
public class EstudianteService {

  private final PersonaRepository personas;
  private final EstudianteRepository estudiantes;
  private final CuentaUsuarioRepository cuentas;
  private final PasswordEncoder passwordEncoder;
  private final pe.edu.pucp.colegio.common.repository.MatriculaRepository matriculas;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;

  public EstudianteService(
      PersonaRepository personas,
      EstudianteRepository estudiantes,
      CuentaUsuarioRepository cuentas,
      PasswordEncoder passwordEncoder,
      pe.edu.pucp.colegio.common.repository.MatriculaRepository matriculas,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos) {
    this.personas = personas;
    this.estudiantes = estudiantes;
    this.cuentas = cuentas;
    this.passwordEncoder = passwordEncoder;
    this.matriculas = matriculas;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
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
    var porEstudiante = new java.util.HashMap<Integer, java.util.List<MatriculaResponse>>();
    if (!ids.isEmpty()) {
      for (Matricula matricula : matriculas.findAllByEstudianteIdIn(ids)) {
        porEstudiante
            .computeIfAbsent(
                matricula.getEstudiante().getId(), ignored -> new java.util.ArrayList<>())
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
              porEstudiante.getOrDefault(estudiante.getId(), java.util.List.of()));
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
        persona.getNombreCompleto());
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
  public EstudianteResponse asignarCuentaExistente(Integer cuentaUsuarioId) {
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

    cuenta.setRol(Rol.ALUMNO);
    cuentas.save(cuenta);

    return new EstudianteResponse(
        estudiante.getId(),
        persona.getId(),
        cuenta.getUsername(),
        cuenta.getRol().name(),
        persona.getDni(),
        persona.getNombreCompleto());
  }
}
