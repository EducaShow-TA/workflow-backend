package pe.edu.pucp.colegio.security.service.impl;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Parentesco;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Persiste una fila independiente para que un error no revierta el resto de la carga. */
/** Procesa una fila CSV y crea o actualiza los perfiles asociados al usuario. */
@Service
public class ProcesadorFilaCargaMasiva {

  private static final String ALFABETO =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
  private static final int LARGO_CONTRASENHA = 12;
  private static final SecureRandom ALEATORIO = new SecureRandom();

  private final PersonaRepository personas;
  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final PasswordEncoder encoder;

  public ProcesadorFilaCargaMasiva(
      PersonaRepository personas,
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      PasswordEncoder encoder) {
    this.personas = personas;
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
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
    String username =
        fila.username() == null
            ? (cuenta == null ? correo : cuenta.getUsername())
            : fila.username().trim();
    String contrasenhaTemporal = null;
    if (cuentaCreada) {
      contrasenhaTemporal = generarContrasenha();
      cuenta =
          cuentas.save(
              new CuentaUsuario(username, encoder.encode(contrasenhaTemporal), rol, persona));
    } else if (!username.equals(cuenta.getUsername())) {
      cuenta.setUsername(username);
      cuentas.save(cuenta);
      datosActualizados = true;
    }

    boolean vinculoCreado = false;
    if (rol == Rol.APODERADO) {
      Apoderado apoderado = apoderados.findByPersonaDni(fila.dni()).orElseGet(Apoderado::new);
      if (apoderado.getPersona() == null) {
        apoderado.setPersona(persona);
        apoderados.save(apoderado);
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
      VinculoFamiliar vinculo =
          vinculos
              .findByEstudianteIdAndApoderadoId(estudiante.getId(), apoderado.getId())
              .orElse(null);
      for (VinculoFamiliar existente : vinculos.findAllByEstudianteId(estudiante.getId())) {
        if (!existente.getApoderado().getId().equals(apoderado.getId())
            && existente.isEsPrincipal()) {
          existente.setEsPrincipal(false);
          vinculos.save(existente);
        }
      }
      if (vinculo == null) {
        vinculo = new VinculoFamiliar();
        vinculo.setEstudiante(estudiante);
        vinculo.setApoderado(apoderado);
        vinculo.setParentesco(
            Parentesco.valueOf(fila.parentesco().trim().toUpperCase(Locale.ROOT)));
        vinculo.setEsPrincipal(true);
        vinculos.save(vinculo);
        vinculoCreado = true;
      } else {
        boolean cambio = false;
        if (!vinculo.getApoderado().getId().equals(apoderado.getId())) {
          vinculo.setApoderado(apoderado);
          cambio = true;
        }
        Parentesco parentesco =
            Parentesco.valueOf(fila.parentesco().trim().toUpperCase(Locale.ROOT));
        if (vinculo.getParentesco() != parentesco) {
          vinculo.setParentesco(parentesco);
          cambio = true;
        }
        if (!vinculo.isEsPrincipal()) {
          vinculo.setEsPrincipal(true);
          cambio = true;
        }
        if (!Boolean.TRUE.equals(vinculo.getActive())) {
          vinculo.setActive(true);
          cambio = true;
        }
        if (cambio) {
          vinculos.save(vinculo);
          vinculoCreado = true;
        }
      }
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

  private static String generarContrasenha() {
    StringBuilder clave = new StringBuilder(LARGO_CONTRASENHA);
    for (int i = 0; i < LARGO_CONTRASENHA; i++) {
      clave.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
    }
    return clave.toString();
  }
}
