package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.HorarioEstudianteResponse;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Consulta las clases del horario de la sección confirmada del alumno autenticado. */
@Service
public class MiHorarioEstudianteService {

  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final SesionHorarioRepository sesiones;

  public MiHorarioEstudianteService(
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      SesionHorarioRepository sesiones) {
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.sesiones = sesiones;
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

    LocalDate desde = LocalDate.of(anio, 1, 1);
    LocalDate hasta = LocalDate.of(anio, 12, 31);
    return sesiones
        .listarHorarioEstudiante(
            cuenta.getPersona().getId(), anio, desde, hasta, EstadoMatricula.CONFIRMADA)
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
