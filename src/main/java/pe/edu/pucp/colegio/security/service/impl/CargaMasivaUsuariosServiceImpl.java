package pe.edu.pucp.colegio.security.service.impl;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Parentesco;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.PersonaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;
import pe.edu.pucp.colegio.security.dto.CuentaCargadaDTO;
import pe.edu.pucp.colegio.security.dto.ErrorFilaDTO;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.service.CargaMasivaUsuariosService;
import pe.edu.pucp.colegio.security.service.LectorCsvUsuarios;

/**
 * Implementa la carga masiva en dos fases: primero valida todas las filas sin escribir nada y, solo
 * si no hay errores, persiste todo en una transacción. Así un archivo con un error no deja cuentas
 * a medias y los alumnos pueden referirse a apoderados que están en el mismo archivo.
 */
@Service
public class CargaMasivaUsuariosServiceImpl implements CargaMasivaUsuariosService {

  private static final Logger log = LoggerFactory.getLogger(CargaMasivaUsuariosServiceImpl.class);

  private static final Set<Rol> ROLES_PERMITIDOS =
      EnumSet.of(Rol.DOCENTE, Rol.ALUMNO, Rol.APODERADO);
  private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final String ALFABETO =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
  private static final int LARGO_CONTRASENHA = 12;
  private static final SecureRandom ALEATORIO = new SecureRandom();

  private final LectorCsvUsuarios lector;
  private final PersonaRepository personas;
  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final RegistroAuditoriaRepository auditorias;
  private final PasswordEncoder encoder;

  public CargaMasivaUsuariosServiceImpl(
      LectorCsvUsuarios lector,
      PersonaRepository personas,
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      RegistroAuditoriaRepository auditorias,
      PasswordEncoder encoder) {
    this.lector = lector;
    this.personas = personas;
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.auditorias = auditorias;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public CargaMasivaResultadoDTO cargar(byte[] contenido, String usernameResponsable) {
    List<FilaUsuarioCsv> filas = lector.leer(contenido);
    List<ErrorFilaDTO> errores = validar(filas);
    if (!errores.isEmpty()) {
      log.warn("event=carga_masiva_rechazada filas={} errores={}", filas.size(), errores.size());
      return new CargaMasivaResultadoDTO(false, filas.size(), errores, List.of());
    }
    List<CuentaCargadaDTO> creadas = persistir(filas);
    var responsable = cuentas.findByUsername(usernameResponsable).orElse(null);
    auditorias.save(
        new RegistroAuditoria(
            responsable == null ? null : responsable.getId(),
            responsable == null ? null : responsable.getRol().name(),
            AccionAuditoria.CREAR,
            "Carga masiva de usuarios: " + creadas.size() + " cuentas"));
    log.info(
        "event=carga_masiva_completada cuentas={} por={}", creadas.size(), usernameResponsable);
    return new CargaMasivaResultadoDTO(true, filas.size(), List.of(), creadas);
  }

  // ---------- fase 1: validación (no escribe en la base) ----------

  private List<ErrorFilaDTO> validar(List<FilaUsuarioCsv> filas) {
    List<ErrorFilaDTO> errores = new ArrayList<>();
    Set<String> dnisVistos = new HashSet<>();
    Set<String> correosVistos = new HashSet<>();
    Set<String> dnisApoderadosArchivo = new HashSet<>();
    for (FilaUsuarioCsv f : filas) {
      if (rolDe(f) == Rol.APODERADO && f.dni() != null) {
        dnisApoderadosArchivo.add(f.dni());
      }
    }
    for (FilaUsuarioCsv f : filas) {
      validarFila(f, errores, dnisVistos, correosVistos, dnisApoderadosArchivo);
    }
    return errores;
  }

  private void validarFila(
      FilaUsuarioCsv f,
      List<ErrorFilaDTO> errores,
      Set<String> dnisVistos,
      Set<String> correosVistos,
      Set<String> dnisApoderadosArchivo) {
    int n = f.fila();
    Rol rol = rolDe(f);
    if (rol == null) {
      agregar(errores, n, "rol", "Rol inválido o no permitido; use DOCENTE, ALUMNO o APODERADO");
    }
    validarTexto(errores, n, "dni", f.dni(), 15, true);
    validarTexto(errores, n, "nombres", f.nombres(), 100, true);
    validarTexto(errores, n, "apellido_paterno", f.apellidoPaterno(), 100, true);
    validarTexto(errores, n, "apellido_materno", f.apellidoMaterno(), 100, true);
    validarTexto(errores, n, "telefono", f.telefono(), 20, false);
    validarCorreo(errores, n, f.correo());
    validarFecha(errores, n, f.fechaNacimiento());
    validarDuplicados(errores, f, dnisVistos, correosVistos);
    if (rol == Rol.ALUMNO) {
      validarVinculo(errores, f, dnisApoderadosArchivo);
    }
  }

  private void validarDuplicados(
      List<ErrorFilaDTO> errores,
      FilaUsuarioCsv f,
      Set<String> dnisVistos,
      Set<String> correosVistos) {
    if (f.dni() != null) {
      if (!dnisVistos.add(f.dni())) {
        agregar(errores, f.fila(), "dni", "DNI repetido dentro del archivo");
      } else if (personas.existsByDni(f.dni())) {
        agregar(errores, f.fila(), "dni", "Ya existe una persona con ese DNI");
      }
    }
    if (f.correo() != null) {
      String correo = f.correo().toLowerCase(Locale.ROOT);
      if (!correosVistos.add(correo)) {
        agregar(errores, f.fila(), "correo", "Correo repetido dentro del archivo");
      } else if (cuentas.existsByUsername(correo)) {
        agregar(errores, f.fila(), "correo", "Ya existe una cuenta con ese correo");
      }
    }
  }

  private void validarVinculo(
      List<ErrorFilaDTO> errores, FilaUsuarioCsv f, Set<String> dnisApoderadosArchivo) {
    if (f.dniApoderado() == null) {
      agregar(errores, f.fila(), "dni_apoderado", "Es obligatorio para un ALUMNO");
    } else if (!dnisApoderadosArchivo.contains(f.dniApoderado())
        && !apoderados.existsByPersonaDni(f.dniApoderado())) {
      agregar(
          errores,
          f.fila(),
          "dni_apoderado",
          "No existe un apoderado con ese DNI ni está en el archivo");
    }
    if (f.parentesco() == null) {
      agregar(errores, f.fila(), "parentesco", "Es obligatorio para un ALUMNO");
    } else if (parentescoDe(f) == null) {
      agregar(
          errores, f.fila(), "parentesco", "Valor inválido; use PADRE, MADRE, TUTOR_LEGAL u OTRO");
    }
  }

  private static void validarTexto(
      List<ErrorFilaDTO> errores,
      int fila,
      String campo,
      String valor,
      int maximo,
      boolean obligatorio) {
    if (valor == null) {
      if (obligatorio) {
        agregar(errores, fila, campo, "Es obligatorio");
      }
    } else if (valor.length() > maximo) {
      agregar(errores, fila, campo, "Máximo " + maximo + " caracteres");
    }
  }

  private static void validarCorreo(List<ErrorFilaDTO> errores, int fila, String correo) {
    if (correo == null) {
      agregar(errores, fila, "correo", "Es obligatorio");
    } else if (correo.length() > 50 || !CORREO.matcher(correo).matches()) {
      agregar(errores, fila, "correo", "Correo inválido (máximo 50 caracteres)");
    }
  }

  private static void validarFecha(List<ErrorFilaDTO> errores, int fila, String fecha) {
    if (fecha == null) {
      return;
    }
    try {
      LocalDate.parse(fecha);
    } catch (DateTimeParseException e) {
      agregar(errores, fila, "fecha_nacimiento", "Use el formato AAAA-MM-DD");
    }
  }

  private static void agregar(List<ErrorFilaDTO> errores, int fila, String campo, String mensaje) {
    errores.add(new ErrorFilaDTO(fila, campo, mensaje));
  }

  // ---------- fase 2: persistencia (una sola transacción) ----------

  private List<CuentaCargadaDTO> persistir(List<FilaUsuarioCsv> filas) {
    List<FilaUsuarioCsv> apoderadosPrimero =
        filas.stream()
            .sorted(Comparator.comparing((FilaUsuarioCsv f) -> rolDe(f) != Rol.APODERADO))
            .toList();
    Map<String, Apoderado> apoderadosCreados = new HashMap<>();
    Map<Integer, CuentaCargadaDTO> porFila = new TreeMap<>();
    for (FilaUsuarioCsv f : apoderadosPrimero) {
      Rol rol = rolDe(f);
      Persona persona = personas.save(nuevaPersona(f));
      switch (rol) {
        case APODERADO -> {
          Apoderado apoderado = new Apoderado();
          apoderado.setPersona(persona);
          apoderadosCreados.put(f.dni(), apoderados.save(apoderado));
        }
        case ALUMNO -> crearAlumno(f, persona, apoderadosCreados);
        default -> {
          // DOCENTE: por ahora solo tiene persona y cuenta (aún no existe la entidad Docente).
        }
      }
      String contrasenha = generarContrasenha();
      String correo = f.correo().toLowerCase(Locale.ROOT);
      cuentas.save(new CuentaUsuario(correo, encoder.encode(contrasenha), rol, persona));
      porFila.put(
          f.fila(), new CuentaCargadaDTO(f.fila(), rol.name(), f.dni(), correo, contrasenha));
    }
    return new ArrayList<>(porFila.values());
  }

  private void crearAlumno(
      FilaUsuarioCsv f, Persona persona, Map<String, Apoderado> apoderadosCreados) {
    Estudiante estudiante = new Estudiante();
    estudiante.setPersona(persona);
    estudiante = estudiantes.save(estudiante);
    Apoderado apoderado = apoderadosCreados.get(f.dniApoderado());
    if (apoderado == null) {
      apoderado = apoderados.findByPersonaDni(f.dniApoderado()).orElseThrow();
    }
    VinculoFamiliar vinculo = new VinculoFamiliar();
    vinculo.setParentesco(parentescoDe(f));
    vinculo.setEsPrincipal(true);
    vinculo.setApoderado(apoderado);
    vinculo.setEstudiante(estudiante);
    vinculos.save(vinculo);
  }

  private static Persona nuevaPersona(FilaUsuarioCsv f) {
    Persona persona = new Persona(f.dni(), f.nombres(), f.apellidoPaterno(), f.apellidoMaterno());
    persona.setTelefono(f.telefono());
    if (f.fechaNacimiento() != null) {
      persona.setFechaNacimiento(LocalDate.parse(f.fechaNacimiento()));
    }
    return persona;
  }

  private static String generarContrasenha() {
    StringBuilder clave = new StringBuilder(LARGO_CONTRASENHA);
    for (int i = 0; i < LARGO_CONTRASENHA; i++) {
      clave.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
    }
    return clave.toString();
  }

  // ---------- conversiones ----------

  private static Rol rolDe(FilaUsuarioCsv f) {
    if (f.rol() == null) {
      return null;
    }
    try {
      Rol rol = Rol.valueOf(f.rol().trim().toUpperCase(Locale.ROOT));
      return ROLES_PERMITIDOS.contains(rol) ? rol : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static Parentesco parentescoDe(FilaUsuarioCsv f) {
    if (f.parentesco() == null) {
      return null;
    }
    try {
      return Parentesco.valueOf(f.parentesco().trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
