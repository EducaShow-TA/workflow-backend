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
import pe.edu.pucp.colegio.common.dto.CatalogoLibretaSnapshot;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.AreaLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.CompetenciaLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.EvaluacionLibreta;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse.PeriodoLibreta;
import pe.edu.pucp.colegio.common.model.Calificacion;
import pe.edu.pucp.colegio.common.model.CalificacionParcial;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.EvaluacionParcial;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.CalificacionParcialRepository;
import pe.edu.pucp.colegio.common.repository.CalificacionRepository;
import pe.edu.pucp.colegio.common.repository.DocenteRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.EvaluacionParcialRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

/** Construye la libreta de notas y valida el acceso del usuario solicitante. */
@Service
public class LibretaNotasService {

  private final CuentaUsuarioRepository cuentas;
  private final EstudianteRepository estudiantes;
  private final ApoderadoRepository apoderados;
  private final VinculoFamiliarRepository vinculos;
  private final DocenteRepository docentes;
  private final PeriodoRepository periodos;
  private final MatriculaRepository matriculas;
  private final CatalogoLibretaSnapshotService catalogoLibretaSnapshots;
  private final EvaluacionParcialRepository evaluaciones;
  private final CalificacionRepository calificaciones;
  private final CalificacionParcialRepository notasParciales;
  private final ConsolidacionLogroService consolidacion;
  private final SesionHorarioRepository asignacionesHorario;

  public LibretaNotasService(
      CuentaUsuarioRepository cuentas,
      EstudianteRepository estudiantes,
      ApoderadoRepository apoderados,
      VinculoFamiliarRepository vinculos,
      DocenteRepository docentes,
      PeriodoRepository periodos,
      MatriculaRepository matriculas,
      CatalogoLibretaSnapshotService catalogoLibretaSnapshots,
      EvaluacionParcialRepository evaluaciones,
      CalificacionRepository calificaciones,
      CalificacionParcialRepository notasParciales,
      ConsolidacionLogroService consolidacion,
      SesionHorarioRepository asignacionesHorario) {
    this.cuentas = cuentas;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.docentes = docentes;
    this.periodos = periodos;
    this.matriculas = matriculas;
    this.catalogoLibretaSnapshots = catalogoLibretaSnapshots;
    this.evaluaciones = evaluaciones;
    this.calificaciones = calificaciones;
    this.notasParciales = notasParciales;
    this.consolidacion = consolidacion;
    this.asignacionesHorario = asignacionesHorario;
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

    List<CatalogoLibretaSnapshot> catalogo = catalogoLibretaSnapshots.obtener(periodoSolicitado);
    List<EvaluacionParcial> evaluacionesSinBimestre =
        evaluaciones.findAllByPeriodoIdAndBimestreIsNullAndActiveTrueOrderById(periodoId).stream()
            .filter(
                evaluacion ->
                    competenciasPermitidas == null
                        || competenciasPermitidas.contains(evaluacion.getCompetencia().getCodigo()))
            .toList();
    List<EvaluacionParcial> evaluacionesBimestre =
        evaluaciones
            .findAllByPeriodoIdAndBimestreAndActiveTrueOrderById(periodoId, bimestre)
            .stream()
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
    Map<String, Calificacion> notasManualesPorCompetenciaYBimestre = new HashMap<>();
    calificaciones
        .findAllByPeriodoIdAndMatriculaIdAndActiveTrue(periodoId, matriculaSolicitada.getId())
        .stream()
        .filter(nota -> !Boolean.TRUE.equals(nota.getConsolidada()))
        .forEach(
            nota -> {
              Integer bimestreNota =
                  nota.getBimestre() == null
                      ? BimestreAcademico.actual(
                          periodoSolicitado, nota.getFechaRegistro().toLocalDate())
                      : nota.getBimestre();
              notasManualesPorCompetenciaYBimestre.put(
                  claveManual(nota.getCompetencia().getCodigo(), bimestreNota), nota);
            });
    List<PeriodoLibreta> periodosLibreta =
        List.of(
            periodoLibreta(
                matriculaSolicitada,
                competenciasPermitidas,
                catalogo,
                evaluacionesBimestre,
                notasPorEvaluacion,
                notasManualesPorCompetenciaYBimestre,
                bimestre));
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
      List<CatalogoLibretaSnapshot> catalogo,
      List<EvaluacionParcial> evaluacionesBimestre,
      Map<Integer, CalificacionParcial> notasPorEvaluacion,
      Map<String, Calificacion> notasManualesPorCompetenciaYBimestre,
      Integer bimestre) {
    Periodo periodo = matricula.getPeriodo();
    Map<String, List<EvaluacionParcial>> evaluacionesPorCompetencia = new HashMap<>();
    evaluacionesBimestre.forEach(
        evaluacion ->
            evaluacionesPorCompetencia
                .computeIfAbsent(
                    evaluacion.getCompetencia().getCodigo(), ignored -> new ArrayList<>())
                .add(evaluacion));

    List<AreaLibreta> areasLibreta =
        catalogo.stream()
            .map(
                area -> {
                  List<CompetenciaLibreta> competenciasLibreta =
                      area.competencias().stream()
                          .filter(
                              competencia ->
                                  competenciasPermitidas == null
                                      || competenciasPermitidas.contains(competencia.codigo()))
                          .sorted(
                              Comparator.comparing(
                                  CatalogoLibretaSnapshot.Competencia::nombre,
                                  String.CASE_INSENSITIVE_ORDER))
                          .map(
                              competencia ->
                                  competenciaLibreta(
                                      competencia.codigo(),
                                      competencia.nombre(),
                                      evaluacionesPorCompetencia.getOrDefault(
                                          competencia.codigo(), List.of()),
                                      notasPorEvaluacion,
                                      notasManualesPorCompetenciaYBimestre.get(
                                          claveManual(competencia.codigo(), bimestre))))
                          .toList();
                  return competenciasLibreta.isEmpty()
                      ? null
                      : new AreaLibreta(area.codigo(), area.nombre(), competenciasLibreta);
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
      String competenciaCodigo,
      String competenciaNombre,
      List<EvaluacionParcial> evaluacionesCompetencia,
      Map<Integer, CalificacionParcial> notasPorEvaluacion,
      Calificacion notaManual) {
    if (evaluacionesCompetencia.isEmpty() && notaManual != null) {
      return new CompetenciaLibreta(
          competenciaCodigo,
          competenciaNombre,
          notaManual.getNota(),
          notaManual.getPuntajeConsolidado(),
          notaManual.getObservacion(),
          List.of());
    }
    List<CalificacionParcial> notas =
        evaluacionesCompetencia.stream()
            .map(evaluacion -> notasPorEvaluacion.get(evaluacion.getId()))
            .filter(nota -> nota != null)
            .toList();
    var logro = consolidacion.calcularLogroBimestral(competenciaCodigo, notas);
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
        competenciaCodigo,
        competenciaNombre,
        logro.nota(),
        logro.puntajeConsolidado(),
        null,
        detalle);
  }

  private String claveManual(String competenciaCodigo, Integer bimestre) {
    return competenciaCodigo + ":" + (bimestre == null ? "LEGADO" : bimestre);
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
    var clases =
        asignacionesHorario.buscarAsignacionesDocente(
            cuenta.getPersona().getId(),
            periodo.getSeccion().getId(),
            periodo.getFechaInicio(),
            periodo.getFechaFin());
    if (clases.isEmpty()) {
      throw prohibido("El docente no dicta clases en la sección y período solicitados");
    }
    Set<String> competenciasPermitidas = new HashSet<>();
    clases.forEach(
        asignacion ->
            asignacion.getArea().getCompetencias().stream()
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
