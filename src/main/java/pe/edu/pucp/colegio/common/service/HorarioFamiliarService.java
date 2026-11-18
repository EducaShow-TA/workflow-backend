package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.HorarioFamiliarResponse;
import pe.edu.pucp.colegio.common.model.Apoderado;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.HorarioSeccionRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class HorarioFamiliarService {

  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final MatriculaRepository matriculas;
  private final HorarioSeccionRepository horarios;
  private final SesionHorarioRepository sesiones;
  private final Clock clock;

  public HorarioFamiliarService(
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      MatriculaRepository matriculas,
      HorarioSeccionRepository horarios,
      SesionHorarioRepository sesiones,
      Clock clock) {
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.matriculas = matriculas;
    this.horarios = horarios;
    this.sesiones = sesiones;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<HorarioFamiliarResponse> listar(String username, Integer anioSolicitado) {
    int anio = anioSolicitado == null ? LocalDate.now(clock).getYear() : anioSolicitado;
    if (anio < 2000 || anio > 2100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Año escolar no válido");
    }
    CuentaUsuario cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no válida"));
    List<Estudiante> autorizados = estudiantesAutorizados(cuenta);
    if (autorizados.isEmpty()) {
      return List.of();
    }
    Set<Integer> ids =
        autorizados.stream().map(Estudiante::getId).collect(java.util.stream.Collectors.toSet());
    List<Matricula> todasLasMatriculas = matriculas.findAllByEstudianteIdIn(List.copyOf(ids));
    LocalDate hoy = LocalDate.now(clock);

    return autorizados.stream()
        .sorted(Comparator.comparing(e -> e.getPersona().getNombreCompleto()))
        .flatMap(
            estudiante ->
                todasLasMatriculas.stream()
                    .filter(m -> m.getEstudiante().getId().equals(estudiante.getId()))
                    .filter(m -> Boolean.TRUE.equals(m.getActive()))
                    .filter(m -> m.getEstado() == EstadoMatricula.CONFIRMADA)
                    .filter(m -> Boolean.TRUE.equals(m.getPeriodo().getActive()))
                    .filter(m -> m.getPeriodo().getFechaInicio().getYear() == anio)
                    .max(Comparator.comparing(m -> m.getPeriodo().getFechaInicio()))
                    .stream()
                    .map(m -> transformar(estudiante, m.getPeriodo(), anio, hoy)))
        .toList();
  }

  private List<Estudiante> estudiantesAutorizados(CuentaUsuario cuenta) {
    if (cuenta.getPersona() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta sin persona asociada");
    }
    if (cuenta.getRol() == Rol.ALUMNO) {
      return estudiantes
          .findByPersonaDni(cuenta.getPersona().getDni())
          .filter(e -> Boolean.TRUE.equals(e.getActive()))
          .map(List::of)
          .orElseGet(List::of);
    }
    if (cuenta.getRol() == Rol.APODERADO) {
      Apoderado apoderado =
          apoderados
              .findByPersonaIdAndActiveTrue(cuenta.getPersona().getId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.FORBIDDEN, "No existe un apoderado activo"));
      return vinculos.listarEstudiantesActivosDeApoderado(apoderado.getId()).stream()
          .map(vinculo -> vinculo.getEstudiante())
          .distinct()
          .toList();
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rol sin permiso para horarios");
  }

  private HorarioFamiliarResponse transformar(
      Estudiante estudiante, Periodo periodo, int anio, LocalDate hoy) {
    var horario =
        horarios
            .findBySeccionIdAndAnio(periodo.getSeccion().getId(), anio)
            .filter(h -> Boolean.TRUE.equals(h.getActive()));
    LocalDate fechaReferencia =
        hoy.isBefore(periodo.getFechaInicio())
            ? periodo.getFechaInicio()
            : hoy.isAfter(periodo.getFechaFin()) ? periodo.getFechaFin() : hoy;
    List<HorarioFamiliarResponse.Bloque> bloques =
        horario
            .map(
                h ->
                    sesiones.findAllByHorarioId(h.getId()).stream()
                        .filter(s -> s.estaVigenteEn(fechaReferencia))
                        .sorted(
                            Comparator.comparing(
                                    (pe.edu.pucp.colegio.common.model.SesionHorario s) ->
                                        s.getDia().getValue())
                                .thenComparing(s -> s.getBloque().getHoraInicio()))
                        .map(
                            s ->
                                new HorarioFamiliarResponse.Bloque(
                                    s.getId(),
                                    s.getDia(),
                                    s.getBloque().getHoraInicio().toString(),
                                    s.getBloque().getHoraFin().toString(),
                                    s.getArea().getNombre(),
                                    s.getAula().getCodigo(),
                                    s.getDocente().getPersona().getNombreCompleto()))
                        .toList())
            .orElseGet(List::of);
    return new HorarioFamiliarResponse(
        estudiante.getId(),
        estudiante.getPersona().getNombreCompleto(),
        periodo.getGrado().getNumero(),
        periodo.getSeccion().getCodigo(),
        anio,
        horario.isPresent(),
        bloques);
  }
}
