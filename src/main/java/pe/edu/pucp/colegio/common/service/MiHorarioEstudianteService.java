package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.HorarioEstudianteResponse;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.VinculoFamiliar;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Consulta las clases del horario de la sección confirmada del alumno autenticado. */
@Service
public class MiHorarioEstudianteService {

  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final SesionHorarioRepository sesiones;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;

  public MiHorarioEstudianteService(
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      SesionHorarioRepository sesiones,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos) {
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.sesiones = sesiones;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
  }

  @Transactional(readOnly = true)
  public List<HorarioEstudianteResponse> listar(String username, Integer anio) {
    var cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .filter(usuario -> usuario.getRol() == Rol.ALUMNO && usuario.getPersona() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "La cuenta no corresponde a un alumno activo"));
    estudiantes
        .findByPersonaIdAndActiveTrue(cuenta.getPersona().getId())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No existe un registro de estudiante activo"));

    return listarPorPersona(cuenta.getPersona().getId(), anio);
  }

  @Transactional(readOnly = true)
  public List<HorarioEstudianteResponse> listarParaApoderado(
      String username, Integer estudianteId, Integer anio) {
    var cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .filter(usuario -> usuario.getRol() == Rol.APODERADO && usuario.getPersona() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "La cuenta no corresponde a un apoderado activo"));
    var apoderado =
        apoderados
            .findByPersonaIdAndActiveTrue(cuenta.getPersona().getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No existe un registro de apoderado activo"));
    VinculoFamiliar vinculo =
        vinculos
            .findByEstudianteIdAndApoderadoIdAndActiveTrue(estudianteId, apoderado.getId())
            .filter(v -> Boolean.TRUE.equals(v.getEstudiante().getActive()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "El estudiante no está vinculado a este apoderado"));
    return listarPorPersona(vinculo.getEstudiante().getPersona().getId(), anio);
  }

  private List<HorarioEstudianteResponse> listarPorPersona(Integer personaId, Integer anio) {
    LocalDate desde = LocalDate.of(anio, 1, 1);
    LocalDate hasta = LocalDate.of(anio, 12, 31);
    return sesiones
        .listarHorarioEstudiante(personaId, anio, desde, hasta, EstadoMatricula.CONFIRMADA)
        .stream()
        .map(
            sesion ->
                new HorarioEstudianteResponse(
                    sesion.getId(),
                    sesion.getDia(),
                    sesion.getBloque().getHoraInicio().toString(),
                    sesion.getBloque().getHoraFin().toString(),
                    sesion.getArea().getCodigo(),
                    sesion.getArea().getNombre(),
                    sesion.getDocente().getPersona().getNombreCompleto(),
                    sesion.getHorario().getSeccion().getGrado().getNumero(),
                    sesion.getHorario().getSeccion().getCodigo(),
                    sesion.getAula().getCodigo()))
        .toList();
  }
}
