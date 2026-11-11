package pe.edu.pucp.colegio.common.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.HorarioDocenteResponse;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Consulta el horario semanal real de la cuenta docente autenticada. */
@Service
public class MiHorarioDocenteService {

  private final CuentaUsuarioRepository cuentas;
  private final SesionHorarioRepository sesiones;

  public MiHorarioDocenteService(
      CuentaUsuarioRepository cuentas, SesionHorarioRepository sesiones) {
    this.cuentas = cuentas;
    this.sesiones = sesiones;
  }

  @Transactional(readOnly = true)
  public List<HorarioDocenteResponse> listar(String username, Integer anio) {
    var cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .filter(usuario -> usuario.getRol() == Rol.DOCENTE)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "La cuenta no corresponde a un docente activo"));

    return sesiones.listarHorarioDocente(cuenta.getId(), anio).stream()
        .map(
            sesion ->
                new HorarioDocenteResponse(
                    sesion.getId(),
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
}
