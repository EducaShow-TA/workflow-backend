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
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class EstudianteService {

  private final PersonaRepository personas;
  private final EstudianteRepository estudiantes;
  private final CuentaUsuarioRepository cuentas;
  private final PasswordEncoder passwordEncoder;
  private final pe.edu.pucp.colegio.common.repository.MatriculaRepository matriculas;

  public EstudianteService(
      PersonaRepository personas,
      EstudianteRepository estudiantes,
      CuentaUsuarioRepository cuentas,
      PasswordEncoder passwordEncoder,
      pe.edu.pucp.colegio.common.repository.MatriculaRepository matriculas) {
    this.personas = personas;
    this.estudiantes = estudiantes;
    this.cuentas = cuentas;
    this.passwordEncoder = passwordEncoder;
    this.matriculas = matriculas;
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
    if (personas.existsByDni(request.dni())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una persona con ese DNI");
    }
    if (cuentas.existsByUsername(request.username())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Ya existe una cuenta con ese nombre de usuario");
    }

    Persona persona =
        new Persona(
            request.dni(), request.nombre(), request.apellidoPaterno(), request.apellidoMaterno());
    persona.setFechaNacimiento(request.fechaNacimiento());
    persona.setTelefono(request.telefono());
    persona.setCorreo(request.correo());
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

    return new EstudianteResponse(
        estudiante.getId(),
        persona.getId(),
        cuenta.getUsername(),
        cuenta.getRol().name(),
        persona.getDni(),
        persona.getNombreCompleto());
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
