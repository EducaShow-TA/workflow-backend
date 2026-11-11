package pe.edu.pucp.colegio.common.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.EstudianteVinculadoResponse;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class EstudiantesApoderadoService {

  private final CuentaUsuarioRepository cuentas;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;

  public EstudiantesApoderadoService(
      CuentaUsuarioRepository cuentas,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos) {
    this.cuentas = cuentas;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
  }

  @Transactional(readOnly = true)
  public List<EstudianteVinculadoResponse> listar(String username) {
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

    return vinculos.listarEstudiantesActivosDeApoderado(apoderado.getId()).stream()
        .map(
            vinculo -> {
              var estudiante = vinculo.getEstudiante();
              var periodos =
                  estudiante.getMatriculas().stream()
                      .filter(matricula -> Boolean.TRUE.equals(matricula.getActive()))
                      .filter(matricula -> matricula.getEstado() == EstadoMatricula.CONFIRMADA)
                      .map(
                          matricula -> {
                            var periodo = matricula.getPeriodo();
                            return new EstudianteVinculadoResponse.PeriodoVinculado(
                                periodo.getId(),
                                periodo.getGrado().getNumero(),
                                periodo.getSeccion().getCodigo(),
                                periodo.getFechaInicio(),
                                periodo.getFechaFin());
                          })
                      .sorted(Comparator.comparing(EstudianteVinculadoResponse.PeriodoVinculado::fechaInicio).reversed())
                      .toList();
              return new EstudianteVinculadoResponse(
                  estudiante.getId(), estudiante.getPersona().getNombreCompleto(), periodos);
            })
        .sorted(Comparator.comparing(EstudianteVinculadoResponse::nombreCompleto))
        .toList();
  }
}
