package pe.edu.pucp.colegio.security.service.impl;

import java.time.LocalDate;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Parentesco;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.common.validation.UsernamePolicy;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.service.ContrasenhaTemporal;

/** Procesa una fila CSV y crea o actualiza los perfiles asociados al usuario. */
@Service
public class ProcesadorFilaCargaMasiva {

  private final PersonaRepository personas;
  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final DocenteRepository docentes;
  private final VinculoFamiliarRepository vinculos;
  private final PasswordEncoder encoder;

  public ProcesadorFilaCargaMasiva(
      PersonaRepository personas,
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      DocenteRepository docentes,
      VinculoFamiliarRepository vinculos,
      PasswordEncoder encoder) {
    this.personas = personas;
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.docentes = docentes;
    this.vinculos = vinculos;
    this.encoder = encoder;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ResultadoProcesamientoFila procesar(FilaUsuarioCsv fila) {
    Rol rol = Rol.valueOf(fila.rol().trim().toUpperCase(Locale.ROOT));
    String correo = fila.correo().trim().toLowerCase(Locale.ROOT);
    Persona persona = personas.findByDni(fila.dni()).orElse(null);
    boolean personaNueva = persona == null;
    boolean datosActualizados = false;
    if (personaNueva) {
      persona =
          new Persona(fila.dni(), fila.nombres(), fila.apellidoPaterno(), fila.apellidoMaterno());
    } else {
      if (!fila.nombres().equals(persona.getNombre())) {
        persona.setNombre(fila.nombres());
        datosActualizados = true;
      }
      if (!fila.apellidoPaterno().equals(persona.getApellidoPaterno())) {
        persona.setApellidoPaterno(fila.apellidoPaterno());
        datosActualizados = true;
      }
      if (!fila.apellidoMaterno().equals(persona.getApellidoMaterno())) {
        persona.setApellidoMaterno(fila.apellidoMaterno());
        datosActualizados = true;
      }
    }
    if (!correo.equals(persona.getCorreo())) {
      persona.setCorreo(correo);
      datosActualizados |= !personaNueva;
    }
    if (fila.telefono() != null && !fila.telefono().equals(persona.getTelefono())) {
      persona.setTelefono(fila.telefono());
      datosActualizados |= !personaNueva;
    }
    if (fila.fechaNacimiento() != null) {
      LocalDate fechaNacimiento = LocalDate.parse(fila.fechaNacimiento());
      if (!fechaNacimiento.equals(persona.getFechaNacimiento())) {
        persona.setFechaNacimiento(fechaNacimiento);
        datosActualizados |= !personaNueva;
      }
    }
    persona = personas.save(persona);

    CuentaUsuario cuenta = cuentas.findByPersonaDni(fila.dni()).orElse(null);
    if (cuenta != null && cuenta.getRol() != rol) {
      throw new IllegalArgumentException(
          "El rol del CSV no coincide con el rol de la cuenta existente");
    }
    boolean cuentaCreada = cuenta == null;
    boolean usernameAutomatico = rol == Rol.ALUMNO || rol == Rol.DOCENTE && cuenta == null;
    String username =
        usernameAutomatico || fila.username() == null
            ? (cuenta == null
                ? (rol == Rol.ALUMNO
                    ? UsernamePolicy.generarAlumnoDesdeDocumento(fila.dni())
                    : UsernamePolicy.generarDesdeDocumento(fila.dni()))
                : cuenta.getUsername())
            : fila.username().trim();
    String contrasenhaTemporal = null;
    if (cuentaCreada) {
      contrasenhaTemporal = ContrasenhaTemporal.generar();
      cuenta =
          cuentas.save(
              new CuentaUsuario(username, encoder.encode(contrasenhaTemporal), rol, persona));
    } else if (!username.equals(cuenta.getUsername())) {
      cuenta.setUsername(username);
      cuentas.save(cuenta);
      datosActualizados = true;
    }

    boolean vinculoCreado = false;
    if (rol == Rol.DOCENTE) {
      if (docentes.findByPersonaId(persona.getId()).isEmpty()) {
        Docente docente = new Docente();
        docente.setPersona(persona);
        docentes.save(docente);
      }
    } else if (rol == Rol.APODERADO) {
      Apoderado apoderado = apoderados.findByPersonaDni(fila.dni()).orElseGet(Apoderado::new);
      if (apoderado.getPersona() == null) {
        apoderado.setPersona(persona);
        apoderados.save(apoderado);
      }
      if (fila.dniAlumno() != null) {
        Estudiante estudiante =
            estudiantes
                .findByPersonaDni(fila.dniAlumno())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "No existe el alumno indicado para el apoderado"));
        vinculoCreado =
            vincular(
                estudiante,
                apoderado,
                parentesco(fila.parentesco()),
                esPrincipal(fila.esPrincipal()));
      }
    } else if (rol == Rol.ALUMNO) {
      Estudiante estudiante = estudiantes.findByPersonaDni(fila.dni()).orElseGet(Estudiante::new);
      if (estudiante.getPersona() == null) {
        estudiante.setPersona(persona);
        estudiante = estudiantes.save(estudiante);
      }

      Apoderado apoderado =
          apoderados
              .findByPersonaDni(fila.dniApoderado())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "No existe el apoderado indicado para el alumno"));
      vinculoCreado = vincular(estudiante, apoderado, parentesco(fila.parentesco()), true);
    }
    return new ResultadoProcesamientoFila(
        rol,
        fila.dni(),
        username,
        cuentaCreada,
        datosActualizados,
        vinculoCreado,
        contrasenhaTemporal);
  }

  private boolean vincular(
      Estudiante estudiante, Apoderado apoderado, Parentesco parentesco, boolean esPrincipal) {
    VinculoFamiliar vinculo =
        vinculos
            .findByEstudianteIdAndApoderadoId(estudiante.getId(), apoderado.getId())
            .orElse(null);
    if (esPrincipal) {
      for (VinculoFamiliar existente : vinculos.findAllByEstudianteId(estudiante.getId())) {
        if (!existente.getApoderado().getId().equals(apoderado.getId())
            && existente.isEsPrincipal()) {
          existente.setEsPrincipal(false);
          vinculos.save(existente);
        }
      }
    }
    if (vinculo == null) {
      vinculo = new VinculoFamiliar();
      vinculo.setEstudiante(estudiante);
      vinculo.setApoderado(apoderado);
      vinculo.setParentesco(parentesco);
      vinculo.setEsPrincipal(esPrincipal);
      vinculos.save(vinculo);
      return true;
    }
    boolean cambio = false;
    if (vinculo.getParentesco() != parentesco) {
      vinculo.setParentesco(parentesco);
      cambio = true;
    }
    if (vinculo.isEsPrincipal() != esPrincipal) {
      vinculo.setEsPrincipal(esPrincipal);
      cambio = true;
    }
    if (!Boolean.TRUE.equals(vinculo.getActive())) {
      vinculo.setActive(true);
      cambio = true;
    }
    if (cambio) {
      vinculos.save(vinculo);
    }
    return cambio;
  }

  private static Parentesco parentesco(String valor) {
    return Parentesco.valueOf(valor.trim().toUpperCase(Locale.ROOT));
  }

  private static boolean esPrincipal(String valor) {
    return switch (valor.trim().toUpperCase(Locale.ROOT)) {
      case "SI", "SÍ", "TRUE", "1" -> true;
      default -> false;
    };
  }
}
