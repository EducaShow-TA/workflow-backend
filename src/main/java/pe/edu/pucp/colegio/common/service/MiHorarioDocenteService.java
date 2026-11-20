package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.HorarioDocenteResponse;
import pe.edu.pucp.colegio.common.dto.SesionHorarioDocenteRequest;
import pe.edu.pucp.colegio.common.dto.SesionHorarioRequest;
import pe.edu.pucp.colegio.common.dto.SesionHorarioResponse;
import pe.edu.pucp.colegio.common.model.Docente;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Consulta el horario semanal real de la cuenta docente autenticada. */
@Service
public class MiHorarioDocenteService {

  private final CuentaUsuarioRepository cuentas;
  private final SesionHorarioRepository sesiones;
  private final DocenteRepository docentes;
  private final HorarioSeccionService horariosSeccion;
  private final Clock clock;

  public MiHorarioDocenteService(
      CuentaUsuarioRepository cuentas,
      SesionHorarioRepository sesiones,
      DocenteRepository docentes,
      HorarioSeccionService horariosSeccion,
      Clock clock) {
    this.cuentas = cuentas;
    this.sesiones = sesiones;
    this.docentes = docentes;
    this.horariosSeccion = horariosSeccion;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<HorarioDocenteResponse> listar(String username, Integer anio) {
    Docente docente = docenteAutenticado(username);
    if (docente == null) {
      return List.of();
    }
    LocalDate desde = LocalDate.of(anio, 1, 1);
    LocalDate hasta = LocalDate.of(anio, 12, 31);
    LocalDate hoy = LocalDate.now(clock);
    LocalDate fecha = hoy.isBefore(desde) ? desde : hoy.isAfter(hasta) ? hasta : hoy;
    return sesiones.listarHorarioDocente(docente.getId(), anio, desde, hasta, fecha).stream()
        .map(
            sesion ->
                new HorarioDocenteResponse(
                    sesion.getId(),
                    sesion.getHorario().getId(),
                    sesion.getHorario().getSeccion().getId(),
                    sesion.getBloque().getId(),
                    sesion.getAula().getId(),
                    sesion.getDia(),
                    sesion.getBloque().getHoraInicio().toString(),
                    sesion.getBloque().getHoraFin().toString(),
                    sesion.getArea().getCodigo(),
                    sesion.getArea().getNombre(),
                    sesion.getHorario().getSeccion().getGrado().getNumero(),
                    sesion.getHorario().getSeccion().getCodigo(),
                    sesion.getAula().getCodigo()))
        .toList();
  }

  @Transactional
  public SesionHorarioResponse agregar(
      String username, Integer horarioId, SesionHorarioDocenteRequest request) {
    Docente docente = docenteAutenticado(username);
    if (docente == null) {
      throw sinPerfil();
    }
    if (!sesiones.existsByHorarioIdAndDocenteId(horarioId, docente.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Solo puedes agregar sesiones a secciones donde ya tienes asignaciones");
    }
    return horariosSeccion.agregarSesion(horarioId, convertir(request, docente));
  }

  @Transactional
  public SesionHorarioResponse actualizar(
      String username, Integer sesionId, SesionHorarioDocenteRequest request) {
    Docente docente = docenteAutenticado(username);
    var sesion = buscarSesionPropia(sesionId, docente);
    return horariosSeccion.actualizarSesion(
        sesion.getHorario().getId(), sesionId, convertir(request, docente));
  }

  @Transactional
  public void eliminar(String username, Integer sesionId) {
    Docente docente = docenteAutenticado(username);
    var sesion = buscarSesionPropia(sesionId, docente);
    horariosSeccion.eliminarSesion(sesion.getHorario().getId(), sesionId);
  }

  private Docente docenteAutenticado(String username) {
    var cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .filter(usuario -> usuario.getRol() == Rol.DOCENTE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "La cuenta no corresponde a un docente activo"));
    return cuenta.getPersona() == null
        ? null
        : docentes.findByPersonaId(cuenta.getPersona().getId()).orElse(null);
  }

  private pe.edu.pucp.colegio.common.model.SesionHorario buscarSesionPropia(
      Integer sesionId, Docente docente) {
    if (docente == null) {
      throw sinPerfil();
    }
    var sesion =
        sesiones
            .findById(sesionId)
            .filter(actual -> actual.getDocente().getId().equals(docente.getId()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe una sesión propia con ese identificador"));
    return sesion;
  }

  private SesionHorarioRequest convertir(SesionHorarioDocenteRequest request, Docente docente) {
    return new SesionHorarioRequest(
        request.bloqueId(), request.dia(), request.areaCodigo(), docente.getId(), request.aulaId());
  }

  private ResponseStatusException sinPerfil() {
    return new ResponseStatusException(
        HttpStatus.FORBIDDEN, "La cuenta no tiene un perfil docente activo");
  }
}
