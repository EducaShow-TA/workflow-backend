package pe.edu.pucp.colegio.security.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.validation.UsernamePolicy;
import pe.edu.pucp.colegio.security.dto.CargaMasivaResultadoDTO;
import pe.edu.pucp.colegio.security.dto.CuentaCargadaDTO;
import pe.edu.pucp.colegio.security.dto.ErrorFilaDTO;
import pe.edu.pucp.colegio.security.dto.FilaCargaMasivaResultadoDTO;
import pe.edu.pucp.colegio.security.dto.FilaUsuarioCsv;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;
import pe.edu.pucp.colegio.security.service.CargaMasivaUsuariosService;
import pe.edu.pucp.colegio.security.service.LectorCsvUsuarios;

/** Procesa cada fila válida de forma independiente para conservar los resultados parciales. */
/** Coordina la validación y el procesamiento por filas de archivos CSV de usuarios. */
@Service
public class CargaMasivaUsuariosServiceImpl implements CargaMasivaUsuariosService {

  private static final Logger log = LoggerFactory.getLogger(CargaMasivaUsuariosServiceImpl.class);

  private static final Set<Rol> ROLES_PERMITIDOS =
      EnumSet.of(Rol.DOCENTE, Rol.ALUMNO, Rol.APODERADO);
  private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final LectorCsvUsuarios lector;
  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final RegistroAuditoriaRepository auditorias;
  private final ProcesadorFilaCargaMasiva procesador;

  public CargaMasivaUsuariosServiceImpl(
      LectorCsvUsuarios lector,
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      RegistroAuditoriaRepository auditorias,
      ProcesadorFilaCargaMasiva procesador) {
    this.lector = lector;
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.auditorias = auditorias;
    this.procesador = procesador;
  }

  @Override
  public CargaMasivaResultadoDTO cargar(byte[] contenido, String usernameResponsable) {
    List<FilaUsuarioCsv> filas = lector.leer(contenido);
    Map<Integer, List<ErrorFilaDTO>> errores = validar(filas);
    List<FilaCargaMasivaResultadoDTO> resultados = new ArrayList<>();
    List<CuentaCargadaDTO> cuentasCreadas = new ArrayList<>();
    int usuariosCreados = 0;
    int usuariosActualizados = 0;
    int usuariosVinculados = 0;
    int usuariosSinCambios = 0;

    List<FilaUsuarioCsv> ordenadas =
        filas.stream()
            .sorted(
                Comparator.comparing((FilaUsuarioCsv fila) -> rolDe(fila) != Rol.APODERADO)
                    .thenComparingInt(FilaUsuarioCsv::fila))
            .toList();

    for (FilaUsuarioCsv fila : ordenadas) {
      if (errores.containsKey(fila.fila())) {
        continue;
      }
      if (rolDe(fila) == Rol.ALUMNO && !apoderados.existsByPersonaDni(fila.dniApoderado())) {
        agregar(
            errores,
            fila.fila(),
            "dni_apoderado",
            "No se pudo procesar: el apoderado no está registrado o su fila fue rechazada");
        continue;
      }

      try {
        ResultadoProcesamientoFila procesado = procesador.procesar(fila);
        boolean creado = procesado.cuentaCreada();
        boolean actualizado = procesado.datosActualizados();
        boolean vinculado = procesado.vinculoCreado();
        if (creado) {
          usuariosCreados++;
          cuentasCreadas.add(
              new CuentaCargadaDTO(
                  fila.fila(),
                  procesado.rol().name(),
                  procesado.dni(),
                  procesado.username(),
                  procesado.contrasenhaTemporal()));
        }
        if (actualizado) {
          usuariosActualizados++;
        }
        if (vinculado) {
          usuariosVinculados++;
        }
        if (!creado && !actualizado && !vinculado) {
          usuariosSinCambios++;
        }
        resultados.add(
            new FilaCargaMasivaResultadoDTO(
                fila.fila(),
                estado(creado, actualizado, vinculado),
                procesado.rol().name(),
                procesado.dni(),
                procesado.username()));
      } catch (DataIntegrityViolationException e) {
        log.warn(
            "event=carga_masiva_fila_rechazada fila={} causa=conflicto_integridad", fila.fila());
        agregar(
            errores,
            fila.fila(),
            "datos",
            "Conflicto con datos existentes; revise el DNI, correo o vínculo familiar");
      } catch (RuntimeException e) {
        log.error("event=carga_masiva_fila_fallida fila={}", fila.fila(), e);
        agregar(errores, fila.fila(), "datos", mensajeSeguro(e));
      }
    }

    int filasRechazadas = errores.size();
    int filasProcesadas = resultados.size();
    if (filasProcesadas > 0) {
      var responsable = cuentas.findByUsernameIgnoreCase(usernameResponsable).orElse(null);
      AccionAuditoria accion =
          usuariosCreados > 0 ? AccionAuditoria.CREAR : AccionAuditoria.ACTUALIZAR;
      auditorias.save(
          new RegistroAuditoria(
              responsable == null ? null : responsable.getId(),
              responsable == null ? null : responsable.getRol().name(),
              accion,
              "Carga masiva: "
                  + usuariosCreados
                  + " creados, "
                  + usuariosActualizados
                  + " actualizados, "
                  + usuariosVinculados
                  + " vinculados y "
                  + filasRechazadas
                  + " filas rechazadas"));
    }

    log.info(
        "event=carga_masiva_completada total={} procesadas={} rechazadas={} por={}",
        filas.size(),
        filasProcesadas,
        filasRechazadas,
        usernameResponsable);
    List<ErrorFilaDTO> detalleErrores = errores.values().stream().flatMap(List::stream).toList();
    return new CargaMasivaResultadoDTO(
        detalleErrores.isEmpty(),
        filas.size(),
        usuariosCreados,
        usuariosActualizados,
        usuariosVinculados,
        usuariosSinCambios,
        filasRechazadas,
        detalleErrores,
        cuentasCreadas,
        resultados);
  }

  private Map<Integer, List<ErrorFilaDTO>> validar(List<FilaUsuarioCsv> filas) {
    Map<Integer, List<ErrorFilaDTO>> errores = new LinkedHashMap<>();
    Set<String> dnisVistos = new HashSet<>();
    Set<String> correosVistos = new HashSet<>();
    Set<String> usernamesVistos = new HashSet<>();
    Map<String, FilaUsuarioCsv> apoderadosEnArchivo = new LinkedHashMap<>();

    for (FilaUsuarioCsv fila : filas) {
      validarCampos(fila, errores, dnisVistos, correosVistos, usernamesVistos);
      if (rolDe(fila) == Rol.APODERADO && fila.dni() != null) {
        apoderadosEnArchivo.putIfAbsent(fila.dni(), fila);
      }
    }

    Set<String> apoderadosValidosEnArchivo = new HashSet<>();
    for (Map.Entry<String, FilaUsuarioCsv> entry : apoderadosEnArchivo.entrySet()) {
      if (!errores.containsKey(entry.getValue().fila())) {
        apoderadosValidosEnArchivo.add(entry.getKey());
      }
    }

    for (FilaUsuarioCsv fila : filas) {
      if (rolDe(fila) != Rol.ALUMNO) {
        continue;
      }
      if (fila.dniApoderado() == null) {
        agregar(errores, fila.fila(), "dni_apoderado", "Es obligatorio para un ALUMNO");
      } else if (!apoderados.existsByPersonaDni(fila.dniApoderado())
          && !apoderadosValidosEnArchivo.contains(fila.dniApoderado())) {
        agregar(
            errores,
            fila.fila(),
            "dni_apoderado",
            "No existe un apoderado válido con ese DNI ni una fila válida en el archivo");
      }
      if (fila.parentesco() == null) {
        agregar(errores, fila.fila(), "parentesco", "Es obligatorio para un ALUMNO");
      } else if (parentescoDe(fila) == null) {
        agregar(
            errores,
            fila.fila(),
            "parentesco",
            "Valor inválido; use PADRE, MADRE, TUTOR_LEGAL u OTRO");
      }
    }
    return errores;
  }

  private void validarCampos(
      FilaUsuarioCsv fila,
      Map<Integer, List<ErrorFilaDTO>> errores,
      Set<String> dnisVistos,
      Set<String> correosVistos,
      Set<String> usernamesVistos) {
    Rol rol = rolDe(fila);
    if (rol == null) {
      agregar(
          errores,
          fila.fila(),
          "rol",
          "Rol inválido o no permitido; use DOCENTE, ALUMNO o APODERADO");
    }
    validarTexto(errores, fila.fila(), "dni", fila.dni(), 15, true);
    validarTexto(errores, fila.fila(), "nombres", fila.nombres(), 100, true);
    validarTexto(errores, fila.fila(), "apellido_paterno", fila.apellidoPaterno(), 100, true);
    validarTexto(errores, fila.fila(), "apellido_materno", fila.apellidoMaterno(), 100, true);
    validarTexto(errores, fila.fila(), "telefono", fila.telefono(), 20, false);
    validarCorreo(errores, fila.fila(), fila.correo());
    validarTexto(errores, fila.fila(), "username", fila.username(), 50, false);
    validarFecha(errores, fila.fila(), fila.fechaNacimiento());

    if (fila.dni() != null && !dnisVistos.add(fila.dni())) {
      agregar(errores, fila.fila(), "dni", "DNI repetido dentro del archivo");
    }
    if (fila.correo() != null) {
      String correo = fila.correo().trim().toLowerCase(Locale.ROOT);
      if (!correosVistos.add(correo)) {
        agregar(errores, fila.fila(), "correo", "Correo repetido dentro del archivo");
      }
      validarConflictosExistentes(fila, rol, correo, errores);
    }
    CuentaUsuario cuentaExistente =
        fila.dni() == null ? null : cuentas.findByPersonaDni(fila.dni()).orElse(null);
    String usernameEfectivo =
        fila.username() == null
            ? (cuentaExistente == null
                ? UsernamePolicy.generarDesdeDocumento(fila.dni())
                : cuentaExistente.getUsername())
            : fila.username();
    if (fila.username() != null && !UsernamePolicy.esValido(fila.username().trim())) {
      agregar(errores, fila.fila(), "username", "Use solo letras y números en el usuario");
    }
    if (usernameEfectivo != null
        && !usernamesVistos.add(usernameEfectivo.trim().toLowerCase(Locale.ROOT))) {
      agregar(errores, fila.fila(), "username", "Usuario repetido dentro del archivo");
    }
  }

  private void validarConflictosExistentes(
      FilaUsuarioCsv fila, Rol rol, String correo, Map<Integer, List<ErrorFilaDTO>> errores) {
    if (fila.dni() == null || rol == null) {
      return;
    }
    CuentaUsuario cuentaExistente = cuentas.findByPersonaDni(fila.dni()).orElse(null);
    String username =
        fila.username() == null
            ? (cuentaExistente == null ? correo : cuentaExistente.getUsername())
            : fila.username().trim();
    if (cuentaExistente != null) {
      if (cuentaExistente.getRol() != rol) {
        agregar(
            errores,
            fila.fila(),
            "rol",
            "No se permite cambiar el rol de una cuenta existente mediante la carga");
      }
      if (cuentas.existsByUsernameIgnoreCaseAndIdNot(username, cuentaExistente.getId())) {
        agregar(errores, fila.fila(), "username", "El usuario ya pertenece a otra cuenta");
      }
    } else if (cuentas.findByUsernameIgnoreCase(username).isPresent()) {
      agregar(errores, fila.fila(), "username", "El usuario ya pertenece a otra cuenta");
    }
    boolean correoDeOtraCuenta =
        cuentas.findAllByPersonaCorreoIgnoreCase(correo).stream()
            .anyMatch(c -> cuentaExistente == null || !c.getId().equals(cuentaExistente.getId()));
    if (correoDeOtraCuenta) {
      agregar(errores, fila.fila(), "correo", "El correo ya está asociado a otra cuenta");
    }
    boolean usuarioCoincideConCorreoDeOtraCuenta =
        cuentas.findAllByPersonaCorreoIgnoreCase(username).stream()
            .anyMatch(c -> cuentaExistente == null || !c.getId().equals(cuentaExistente.getId()));
    if (usuarioCoincideConCorreoDeOtraCuenta) {
      agregar(
          errores,
          fila.fila(),
          "username",
          "El usuario coincide con el correo de contacto de otra cuenta");
    }
    boolean correoCoincideConUsuarioDeOtraCuenta =
        cuentas
            .findByUsernameIgnoreCase(correo)
            .filter(c -> cuentaExistente == null || !c.getId().equals(cuentaExistente.getId()))
            .isPresent();
    if (correoCoincideConUsuarioDeOtraCuenta) {
      agregar(errores, fila.fila(), "correo", "El correo coincide con el usuario de otra cuenta");
    }

    if (rol == Rol.ALUMNO && apoderados.existsByPersonaDni(fila.dni())) {
      agregar(errores, fila.fila(), "rol", "La persona ya está registrada como apoderado");
    } else if (rol == Rol.APODERADO && estudiantes.existsByPersonaDni(fila.dni())) {
      agregar(errores, fila.fila(), "rol", "La persona ya está registrada como alumno");
    }
  }

  private static void validarTexto(
      Map<Integer, List<ErrorFilaDTO>> errores,
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

  private static void validarCorreo(
      Map<Integer, List<ErrorFilaDTO>> errores, int fila, String correo) {
    if (correo == null) {
      agregar(errores, fila, "correo", "Es obligatorio");
    } else if (correo.length() > 254 || !CORREO.matcher(correo).matches()) {
      agregar(errores, fila, "correo", "Correo inválido (máximo 254 caracteres)");
    }
  }

  private static void validarFecha(
      Map<Integer, List<ErrorFilaDTO>> errores, int fila, String fecha) {
    if (fecha == null) {
      return;
    }
    try {
      LocalDate.parse(fecha);
    } catch (DateTimeParseException e) {
      agregar(errores, fila, "fecha_nacimiento", "Use el formato AAAA-MM-DD");
    }
  }

  private static String mensajeSeguro(RuntimeException e) {
    if (e instanceof IllegalArgumentException && e.getMessage() != null) {
      return e.getMessage();
    }
    return "No se pudo procesar la fila; revise los datos y sus vínculos";
  }

  private static String estado(boolean creado, boolean actualizado, boolean vinculado) {
    if (creado && vinculado) {
      return "CREADO_Y_VINCULADO";
    }
    if (creado) {
      return "CREADO";
    }
    if (actualizado && vinculado) {
      return "ACTUALIZADO_Y_VINCULADO";
    }
    if (actualizado) {
      return "ACTUALIZADO";
    }
    if (vinculado) {
      return "VINCULADO";
    }
    return "SIN_CAMBIOS";
  }

  private static void agregar(
      Map<Integer, List<ErrorFilaDTO>> errores, int fila, String campo, String mensaje) {
    errores
        .computeIfAbsent(fila, ignorado -> new ArrayList<>())
        .add(new ErrorFilaDTO(fila, campo, mensaje));
  }

  private static Rol rolDe(FilaUsuarioCsv fila) {
    if (fila.rol() == null) {
      return null;
    }
    try {
      Rol rol = Rol.valueOf(fila.rol().trim().toUpperCase(Locale.ROOT));
      return ROLES_PERMITIDOS.contains(rol) ? rol : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static pe.edu.pucp.colegio.common.model.Parentesco parentescoDe(FilaUsuarioCsv fila) {
    try {
      return pe.edu.pucp.colegio.common.model.Parentesco.valueOf(
          fila.parentesco().trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }
}
