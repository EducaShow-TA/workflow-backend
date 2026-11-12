package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.AreaLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.CompetenciaLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.EvaluacionLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.PeriodoLibreta;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.CalificacionParcial;
import pe.edu.pucp.colegio.common.model.Competencia;
import pe.edu.pucp.colegio.common.model.EvaluacionParcial;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.SesionClase;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.CalificacionParcialRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.EvaluacionParcialRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class LibretaNotasService {

  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final DocenteRepository docentes;
  private final PeriodoRepository periodos;
  private final MatriculaRepository matriculas;
  private final AreaRepository areas;
  private final EvaluacionParcialRepository evaluaciones;
  private final CalificacionParcialRepository notasParciales;
  private final ConsolidacionLogroService consolidacion;
  private final SesionClaseRepository sesiones;

  public LibretaNotasService(
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      DocenteRepository docentes,
      PeriodoRepository periodos,
      MatriculaRepository matriculas,
      AreaRepository areas,
      EvaluacionParcialRepository evaluaciones,
      CalificacionParcialRepository notasParciales,
      ConsolidacionLogroService consolidacion,
      SesionClaseRepository sesiones) {
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.docentes = docentes;
    this.periodos = periodos;
    this.matriculas = matriculas;
    this.areas = areas;
    this.evaluaciones = evaluaciones;
    this.notasParciales = notasParciales;
    this.consolidacion = consolidacion;
    this.sesiones = sesiones;
  }

  @Transactional(readOnly = true)
  public LibretaNotasResponse generar(
      Integer estudianteId, Integer periodoId, Integer bimestre, String username) {
    if (bimestre == null || bimestre < 1 || bimestre > 4) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El bimestre debe estar entre 1 y 4");
    }

    CuentaUsuario cuenta =
        cuentas
            .findByUsername(username)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cuenta no válida"));
    var estudiante =
        estudiantes
            .findById(estudianteId)
            .filter(alumno -> Boolean.TRUE.equals(alumno.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el estudiante"));
    Periodo periodoSolicitado =
        periodos
            .findById(periodoId)
            .filter(p -> Boolean.TRUE.equals(p.getActive()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el período"));

    Set<String> competenciasPermitidas =
        switch (cuenta.getRol()) {
          case ADMINISTRADOR -> null;
          case APODERADO -> {
            exigirVinculoActivo(cuenta, estudianteId);
            yield null;
          }
          case ALUMNO -> {
            exigirMismaPersona(cuenta, estudiante.getPersona().getId());
            yield null;
          }
          case DOCENTE -> exigirAccesoDocente(cuenta, periodoSolicitado);
          default -> throw prohibido("Este rol no puede consultar libretas de notas");
        };

    LocalDate inicioAnhio = LocalDate.of(periodoSolicitado.getFechaInicio().getYear(), 1, 1);
    LocalDate finAnhio = LocalDate.of(periodoSolicitado.getFechaInicio().getYear(), 12, 31);
    List<Matricula> matriculasDelAnhio =
        matriculas.buscarParaLibreta(
            estudianteId,
            periodoId,
            EstadoMatricula.CONFIRMADA,
            inicioAnhio,
            finAnhio,
            periodoSolicitado.getFechaInicio());
    Matricula matriculaSolicitada =
        matriculasDelAnhio.stream()
            .filter(m -> m.getPeriodo().getId().equals(periodoId))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El estudiante no tiene matrícula confirmada en el período"));

    List<Area> areasActivas = areas.findAllByActivoTrueOrderByNombreAsc();
    List<EvaluacionParcial> evaluacionesSinBimestre =
        evaluaciones.findAllByPeriodoIdAndBimestreIsNullAndActiveTrueOrderById(periodoId).stream()
            .filter(
                evaluacion ->
                    competenciasPermitidas == null
                        || competenciasPermitidas.contains(evaluacion.getCompetencia().getCodigo()))
            .toList();
    List<EvaluacionParcial> evaluacionesBimestre =
        evaluaciones.findAllByPeriodoIdAndBimestreAndActiveTrueOrderById(periodoId, bimestre).stream()
            .filter(
                evaluacion ->
                    competenciasPermitidas == null
                        || competenciasPermitidas.contains(evaluacion.getCompetencia().getCodigo()))
            .toList();
    List<Integer> evaluacionIds =
        evaluacionesBimestre.stream().map(EvaluacionParcial::getId).toList();
    List<CalificacionParcial> notas =
        evaluacionIds.isEmpty()
            ? List.of()
            : notasParciales.findAllByEvaluacionIdInAndMatriculaIdAndActiveTrue(
                evaluacionIds, matriculaSolicitada.getId());
    Map<Integer, CalificacionParcial> notasPorEvaluacion = new HashMap<>();
    notas.forEach(nota -> notasPorEvaluacion.put(nota.getEvaluacion().getId(), nota));
    List<PeriodoLibreta> periodosLibreta =
        List.of(
            periodoLibreta(
                matriculaSolicitada,
                competenciasPermitidas,
                areasActivas,
                evaluacionesBimestre,
                notasPorEvaluacion));
    return new LibretaNotasResponse(
        estudianteId,
        estudiante.getPersona().getNombreCompleto(),
        periodoId,
        periodoSolicitado.getFechaInicio().getYear(),
        bimestre,
        evaluacionesSinBimestre.size(),
        periodosLibreta);
  }

  private PeriodoLibreta periodoLibreta(
      Matricula matricula,
      Set<String> competenciasPermitidas,
      List<Area> areasActivas,
      List<EvaluacionParcial> evaluacionesBimestre,
      Map<Integer, CalificacionParcial> notasPorEvaluacion) {
    Periodo periodo = matricula.getPeriodo();
    Map<String, List<EvaluacionParcial>> evaluacionesPorCompetencia = new HashMap<>();
    evaluacionesBimestre.forEach(
        evaluacion ->
            evaluacionesPorCompetencia
                .computeIfAbsent(
                    evaluacion.getCompetencia().getCodigo(), ignored -> new ArrayList<>())
                .add(evaluacion));

    List<AreaLibreta> areasLibreta =
        areasActivas.stream()
            .map(
                area -> {
                  List<CompetenciaLibreta> competenciasLibreta =
                      area.getCompetencias().stream()
                          .filter(competencia -> Boolean.TRUE.equals(competencia.getActivo()))
                          .filter(
                              competencia ->
                                  competenciasPermitidas == null
                                      || competenciasPermitidas.contains(competencia.getCodigo()))
                          .sorted(
                              Comparator.comparing(
                                  Competencia::getNombre, String.CASE_INSENSITIVE_ORDER))
                          .map(
                              competencia ->
                                  competenciaLibreta(
                                      competencia,
                                      evaluacionesPorCompetencia.getOrDefault(
                                          competencia.getCodigo(), List.of()),
                                      notasPorEvaluacion))
                          .toList();
                  return competenciasLibreta.isEmpty()
                      ? null
                      : new AreaLibreta(area.getCodigo(), area.getNombre(), competenciasLibreta);
                })
            .filter(area -> area != null)
            .sorted(Comparator.comparing(AreaLibreta::nombre, String.CASE_INSENSITIVE_ORDER))
            .toList();

    return new PeriodoLibreta(
        periodo.getId(),
        periodo.getFechaInicio(),
        periodo.getFechaFin(),
        periodo.getGrado().getNumero(),
        periodo.getSeccion().getCodigo(),
        Boolean.TRUE.equals(periodo.getActasCerradas()),
        areasLibreta);
  }

  private CompetenciaLibreta competenciaLibreta(
      Competencia competencia,
      List<EvaluacionParcial> evaluacionesCompetencia,
      Map<Integer, CalificacionParcial> notasPorEvaluacion) {
    List<CalificacionParcial> notas =
        evaluacionesCompetencia.stream()
            .map(evaluacion -> notasPorEvaluacion.get(evaluacion.getId()))
            .filter(nota -> nota != null)
            .toList();
    var logro = consolidacion.calcularLogroBimestral(competencia.getCodigo(), notas);
    List<EvaluacionLibreta> detalle =
        evaluacionesCompetencia.stream()
            .map(
                evaluacion -> {
                  CalificacionParcial nota = notasPorEvaluacion.get(evaluacion.getId());
                  return new EvaluacionLibreta(
                      evaluacion.getId(),
                      evaluacion.getTitulo(),
                      evaluacion.getPonderacion(),
                      nota == null ? null : nota.getNota(),
                      nota != null && Boolean.TRUE.equals(nota.getJustificada()));
                })
            .toList();
    return new CompetenciaLibreta(
        competencia.getCodigo(),
        competencia.getNombre(),
        logro.nota(),
        logro.puntajeConsolidado(),
        null,
        detalle);
  }

  private Set<String> exigirAccesoDocente(CuentaUsuario cuenta, Periodo periodo) {
    if (cuenta.getPersona() == null) {
      throw prohibido("La cuenta docente no tiene una persona asociada");
    }
    var docente =
        docentes
            .findByPersonaId(cuenta.getPersona().getId())
            .filter(d -> Boolean.TRUE.equals(d.getActive()))
            .orElseThrow(() -> prohibido("La cuenta no corresponde a un docente activo"));
    List<SesionClase> clases =
        sesiones.findAllByDocenteIdAndSeccionIdAndFechaBetweenAndActiveTrue(
            docente.getId(),
            periodo.getSeccion().getId(),
            periodo.getFechaInicio(),
            periodo.getFechaFin());
    if (clases.isEmpty()) {
      throw prohibido("El docente no dicta clases en la sección y período solicitados");
    }
    Set<String> competenciasPermitidas = new HashSet<>();
    clases.forEach(
        sesion ->
            sesion.getArea().getCompetencias().stream()
                .filter(competencia -> Boolean.TRUE.equals(competencia.getActivo()))
                .forEach(competencia -> competenciasPermitidas.add(competencia.getCodigo())));
    return competenciasPermitidas;
  }

  private void exigirVinculoActivo(CuentaUsuario cuenta, Integer estudianteId) {
    if (cuenta.getPersona() == null) {
      throw prohibido("La cuenta de apoderado no tiene una persona asociada");
    }
    var apoderado =
        apoderados
            .findByPersonaIdAndActiveTrue(cuenta.getPersona().getId())
            .orElseThrow(() -> prohibido("La cuenta no corresponde a un apoderado activo"));
    if (!vinculos.existsByEstudianteIdAndApoderadoIdAndActiveTrueAndApoderadoActiveTrue(
        estudianteId, apoderado.getId())) {
      throw prohibido("El apoderado no tiene un vínculo activo con el estudiante");
    }
  }

  private void exigirMismaPersona(CuentaUsuario cuenta, Integer personaId) {
    if (cuenta.getPersona() == null || !cuenta.getPersona().getId().equals(personaId)) {
      throw prohibido("El alumno solo puede consultar su propia libreta");
    }
  }

  private ResponseStatusException prohibido(String mensaje) {
    return new ResponseStatusException(HttpStatus.FORBIDDEN, mensaje);
  }
}
