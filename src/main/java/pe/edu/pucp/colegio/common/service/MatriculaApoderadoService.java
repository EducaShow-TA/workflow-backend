package pe.edu.pucp.colegio.common.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.CompromisoPagoRequest;
import pe.edu.pucp.colegio.common.dto.MatriculaPendienteApoderadoResponse;
import pe.edu.pucp.colegio.common.dto.MatriculaResponse;
import pe.edu.pucp.colegio.common.dto.TarifasMatriculaResponse;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class MatriculaApoderadoService {

  private final CuentaUsuarioRepository cuentas;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final MatriculaRepository matriculas;
  private final MatriculaService servicioMatricula;
  private final AprobacionAnualService aprobacionAnual;
  private final PeriodoMatriculaService periodosMatricula;
  private final ConstanciaMatriculaService constancias;

  public MatriculaApoderadoService(
      CuentaUsuarioRepository cuentas,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      MatriculaRepository matriculas,
      MatriculaService servicioMatricula,
      AprobacionAnualService aprobacionAnual,
      PeriodoMatriculaService periodosMatricula,
      ConstanciaMatriculaService constancias) {
    this.cuentas = cuentas;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.matriculas = matriculas;
    this.servicioMatricula = servicioMatricula;
    this.aprobacionAnual = aprobacionAnual;
    this.periodosMatricula = periodosMatricula;
    this.constancias = constancias;
  }

  @Transactional(readOnly = true)
  public List<MatriculaPendienteApoderadoResponse> listarPendientes(String username) {
    var apoderado = apoderado(username);
    List<Integer> estudiantesVinculados =
        vinculos.listarEstudiantesActivosDeApoderado(apoderado.getId()).stream()
            .map(vinculo -> vinculo.getEstudiante().getId())
            .distinct()
            .toList();
    if (estudiantesVinculados.isEmpty()) {
      return List.of();
    }
    return matriculas.findAllByEstudianteIdIn(estudiantesVinculados).stream()
        .filter(matricula -> Boolean.TRUE.equals(matricula.getActive()))
        .filter(matricula -> matricula.getEstado() == EstadoMatricula.EN_REGISTRO)
        .filter(
            matricula ->
                aprobacionAnual.tieneMatriculaAnterior(
                    matricula.getEstudiante().getId(),
                    matricula.getPeriodo().getFechaInicio().getYear()))
        .map(
            matricula -> {
              int anhio = matricula.getPeriodo().getFechaInicio().getYear();
              TarifasMatriculaResponse tarifas = periodosMatricula.obtenerTarifasParaAnhio(anhio);
              return new MatriculaPendienteApoderadoResponse(
                  matricula.getEstudiante().getId(),
                  matricula.getEstudiante().getPersona().getNombreCompleto(),
                  matricula.getPeriodo().getId(),
                  anhio,
                  matricula.getPeriodo().getGrado().getNumero(),
                  matricula.getPeriodo().getSeccion().getCodigo(),
                  tarifas.montoMatricula(),
                  tarifas.montoPension());
            })
        .sorted(
            Comparator.comparing(MatriculaPendienteApoderadoResponse::anhioEscolar)
                .thenComparing(MatriculaPendienteApoderadoResponse::estudiante))
        .toList();
  }

  @Transactional
  public MatriculaResponse confirmar(
      String username, Integer periodoId, Integer estudianteId, CompromisoPagoRequest compromiso) {
    var apoderado = apoderado(username);
    if (!vinculos.existsByEstudianteIdAndApoderadoIdAndActiveTrueAndApoderadoActiveTrue(
        estudianteId, apoderado.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "No tienes un vínculo activo con este estudiante");
    }
    Matricula matricula =
        matriculas
            .findByPeriodoIdAndEstudianteId(periodoId, estudianteId)
            .filter(m -> Boolean.TRUE.equals(m.getActive()))
            .filter(m -> m.getEstado() == EstadoMatricula.EN_REGISTRO)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No hay una matrícula pendiente para confirmar"));
    int anhioEscolar = matricula.getPeriodo().getFechaInicio().getYear();
    if (!aprobacionAnual.tieneMatriculaAnterior(estudianteId, anhioEscolar)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "La matrícula inicial debe ser registrada y confirmada por el administrador");
    }
    aprobacionAnual.validarDestino(estudianteId, matricula.getPeriodo());
    return servicioMatricula.confirmarDesdePortalApoderado(periodoId, estudianteId, compromiso);
  }

  @Transactional(readOnly = true)
  public byte[] constancia(String username, Integer matriculaId) {
    var apoderado = apoderado(username);
    Matricula matricula =
        matriculas
            .findByIdAndActiveTrue(matriculaId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la matrícula"));
    if (!vinculos.existsByEstudianteIdAndApoderadoIdAndActiveTrueAndApoderadoActiveTrue(
        matricula.getEstudiante().getId(), apoderado.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "No tienes un vínculo activo con este estudiante");
    }
    return constancias.generar(matriculaId);
  }

  private pe.edu.pucp.colegio.common.model.Apoderado apoderado(String username) {
    CuentaUsuario cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .filter(usuario -> usuario.getRol() == Rol.APODERADO && usuario.getPersona() != null)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "La cuenta no corresponde a un apoderado activo"));
    return apoderados
        .findByPersonaIdAndActiveTrue(cuenta.getPersona().getId())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No existe un registro de apoderado activo"));
  }
}
