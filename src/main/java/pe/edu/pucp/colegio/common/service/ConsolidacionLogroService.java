package pe.edu.pucp.colegio.common.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.audit.AccionAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoria;
import pe.edu.pucp.colegio.common.audit.RegistroAuditoriaRepository;
import pe.edu.pucp.colegio.common.dto.*;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.*;
import pe.edu.pucp.colegio.common.repository.*;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@Service
public class ConsolidacionLogroService {

  private static final BigDecimal VALOR_C = BigDecimal.ONE;
  private static final BigDecimal VALOR_B = BigDecimal.valueOf(2);
  private static final BigDecimal VALOR_A = BigDecimal.valueOf(3);
  private static final BigDecimal VALOR_AD = BigDecimal.valueOf(4);
  private static final BigDecimal UMBRAL_B = new BigDecimal("1.50");
  private static final BigDecimal UMBRAL_A = new BigDecimal("2.50");
  private static final BigDecimal UMBRAL_AD = new BigDecimal("3.50");

  private final ReglaConsolidacionRepository reglas;
  private final EvaluacionParcialRepository evaluaciones;
  private final CalificacionParcialRepository notasParciales;
  private final CalificacionRepository calificaciones;
  private final PeriodoRepository periodos;
  private final CompetenciaRepository competencias;
  private final MatriculaRepository matriculas;
  private final CuentaUsuarioRepository usuarios;
  private final RegistroAuditoriaRepository auditoria;
  private final AuditoriaCalificacionService auditoriaCalificaciones;
  private final DocenteService docentes;
  private final SesionHorarioRepository asignacionesHorario;
  private final EvidenciaStorageService storage;

  public ConsolidacionLogroService(
      ReglaConsolidacionRepository reglas,
      EvaluacionParcialRepository evaluaciones,
      CalificacionParcialRepository notasParciales,
      CalificacionRepository calificaciones,
      PeriodoRepository periodos,
      CompetenciaRepository competencias,
      MatriculaRepository matriculas,
      CuentaUsuarioRepository usuarios,
      RegistroAuditoriaRepository auditoria,
      AuditoriaCalificacionService auditoriaCalificaciones,
      DocenteService docentes,
      SesionHorarioRepository asignacionesHorario,
      EvidenciaStorageService storage) {
    this.reglas = reglas;
    this.evaluaciones = evaluaciones;
    this.notasParciales = notasParciales;
    this.calificaciones = calificaciones;
    this.periodos = periodos;
    this.competencias = competencias;
    this.matriculas = matriculas;
    this.usuarios = usuarios;
    this.auditoria = auditoria;
    this.auditoriaCalificaciones = auditoriaCalificaciones;
    this.docentes = docentes;
    this.asignacionesHorario = asignacionesHorario;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public ReglaConsolidacionResponse obtenerRegla(String competenciaCodigo) {
    buscarCompetencia(competenciaCodigo);
    return reglas
        .findByCompetenciaCodigo(competenciaCodigo)
        .map(this::respuestaRegla)
        .orElseGet(
            () ->
                new ReglaConsolidacionResponse(
                    competenciaCodigo,
                    VALOR_C,
                    VALOR_B,
                    VALOR_A,
                    VALOR_AD,
                    UMBRAL_B,
                    UMBRAL_A,
                    UMBRAL_AD));
  }

  @Transactional(readOnly = true)
  public ConsolidacionCalculada calcularLogroBimestral(
      String competenciaCodigo, List<CalificacionParcial> notas) {
    return calcularResultado(notas, obtenerRegla(competenciaCodigo));
  }

  @Transactional
  public ReglaConsolidacionResponse actualizarRegla(
      String competenciaCodigo, ReglaConsolidacionRequest request, String username) {
    buscarCompetencia(competenciaCodigo);
    validarUmbrales(request.umbralB(), request.umbralA(), request.umbralAd());

    ReglaConsolidacion regla =
        reglas.findByCompetenciaCodigo(competenciaCodigo).orElseGet(ReglaConsolidacion::new);
    regla.setCompetenciaCodigo(competenciaCodigo);
    regla.setUmbralB(request.umbralB());
    regla.setUmbralA(request.umbralA());
    regla.setUmbralAd(request.umbralAd());
    regla = reglas.save(regla);

    for (Integer periodoId :
        evaluaciones.findDistinctPeriodoIdsByCompetenciaCodigo(competenciaCodigo)) {
      Periodo periodo = periodos.findByIdAndActiveTrueForUpdate(periodoId).orElse(null);
      if (periodo != null && !Boolean.TRUE.equals(periodo.getActasCerradas())) {
        recalcularCompetencia(
            periodo, competenciaCodigo, false, username, "ADMINISTRADOR", "RECALCULAR_REGLA");
      }
    }
    return respuestaRegla(regla);
  }

  @Transactional
  public EvaluacionParcialResponse crearEvaluacion(
      EvaluacionParcialRequest request, String username) {
    Periodo periodo = periodoAbierto(request.periodoId());
    Competencia competencia = buscarCompetencia(request.competenciaCodigo());
    validarCompetenciaActiva(competencia);
    EvaluacionParcial evaluacion = new EvaluacionParcial();
    evaluacion.setPeriodo(periodo);
    evaluacion.setCompetencia(competencia);
    evaluacion.setTitulo(request.titulo().trim());
    evaluacion.setDescripcion(limpiar(request.descripcion()));
    evaluacion.setPonderacion(request.ponderacion());
    evaluacion.setBimestre(request.bimestre());
    evaluacion = evaluaciones.save(evaluacion);
    recalcularCompetencia(
        periodo,
        competencia.getCodigo(),
        false,
        username,
        "ADMINISTRADOR",
        "RECALCULAR_EVALUACION");
    return respuestaEvaluacion(evaluacion);
  }

  @Transactional
  public EvaluacionParcialResponse actualizarEvaluacion(
      Integer evaluacionId, EvaluacionParcialActualizacionRequest request, String username) {
    EvaluacionParcial evaluacion = evaluacionActiva(evaluacionId);
    Periodo periodo = periodoAbierto(evaluacion.getPeriodo().getId());
    evaluacion = evaluacionActiva(evaluacionId);
    evaluacion.setPeriodo(periodo);
    evaluacion.setTitulo(request.titulo().trim());
    evaluacion.setDescripcion(limpiar(request.descripcion()));
    evaluacion.setPonderacion(request.ponderacion());
    evaluacion.setBimestre(request.bimestre());
    evaluacion = evaluaciones.save(evaluacion);
    recalcularCompetencia(
        periodo,
        evaluacion.getCompetencia().getCodigo(),
        false,
        username,
        "ADMINISTRADOR",
        "RECALCULAR_EVALUACION");
    return respuestaEvaluacion(evaluacion);
  }

  @Transactional
  public void eliminarEvaluacion(Integer evaluacionId, String username) {
    EvaluacionParcial evaluacion = evaluacionActiva(evaluacionId);
    Periodo periodo = periodoAbierto(evaluacion.getPeriodo().getId());
    evaluacion = evaluacionActiva(evaluacionId);
    evaluacion.setActive(false);
    evaluaciones.save(evaluacion);
    List<CalificacionParcial> notas =
        notasParciales.findAllByEvaluacionIdAndActiveTrueOrderByMatriculaId(evaluacionId);
    for (CalificacionParcial nota : notas) {
      var anterior = auditoriaCalificaciones.capturarParcial(nota);
      nota.setActive(false);
      auditoriaCalificaciones.registrarParcial(
          username, "ADMINISTRADOR", "DESACTIVAR_EVALUACION", anterior, nota);
    }
    notasParciales.saveAll(notas);
    recalcularCompetencia(
        periodo,
        evaluacion.getCompetencia().getCodigo(),
        false,
        username,
        "ADMINISTRADOR",
        "RECALCULAR_EVALUACION");
  }

  @Transactional(readOnly = true)
  public List<EvaluacionParcialResponse> listarEvaluaciones(
      Integer periodoId, String competenciaCodigo, String username, boolean administrador) {
    Periodo periodo = periodo(periodoId);
    buscarCompetencia(competenciaCodigo);
    exigirAccesoDocente(username, administrador, periodo, competenciaCodigo);
    return evaluaciones
        .findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrueOrderById(periodoId, competenciaCodigo)
        .stream()
        .map(this::respuestaEvaluacion)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CalificacionParcialResponse> listarCalificacionesParciales(
      Integer evaluacionId, String username, boolean administrador) {
    EvaluacionParcial evaluacion = evaluacionActiva(evaluacionId);
    exigirAccesoDocente(
        username, administrador, evaluacion.getPeriodo(), evaluacion.getCompetencia().getCodigo());
    List<Matricula> roster =
        matriculas.findAllByPeriodoIdAndActiveTrueAndEstadoIn(
            evaluacion.getPeriodo().getId(), List.of(EstadoMatricula.CONFIRMADA));
    Map<Integer, CalificacionParcial> registradas = new HashMap<>();
    notasParciales
        .findAllByEvaluacionIdAndActiveTrueOrderByMatriculaId(evaluacionId)
        .forEach(nota -> registradas.put(nota.getMatricula().getId(), nota));
    return roster.stream()
        .map(matricula -> respuestaNotaParcial(matricula, registradas.get(matricula.getId())))
        .toList();
  }

  @Transactional
  public CalificacionParcialResponse registrarCalificacionParcial(
      Integer evaluacionId,
      Integer matriculaId,
      CalificacionParcialRequest request,
      String username,
      boolean administrador) {
    return registrarCalificacionParcial(
        evaluacionId, matriculaId, request, null, username, administrador);
  }

  @Transactional
  public CalificacionParcialResponse registrarCalificacionParcial(
      Integer evaluacionId,
      Integer matriculaId,
      CalificacionParcialRequest request,
      MultipartFile archivo,
      String username,
      boolean administrador) {
    EvaluacionParcial evaluacion = evaluacionActiva(evaluacionId);
    Periodo periodo = periodoAbierto(evaluacion.getPeriodo().getId());
    evaluacion = evaluacionActiva(evaluacionId);
    exigirAccesoDocente(username, administrador, periodo, evaluacion.getCompetencia().getCodigo());
    Matricula matricula =
        matriculas
            .findById(matriculaId)
            .filter(Matricula::getActive)
            .filter(m -> m.getEstado() == EstadoMatricula.CONFIRMADA)
            .filter(m -> m.getPeriodo().getId().equals(periodo.getId()))
            .orElseThrow(
                () ->
                    new BusinessException(
                        "La matrícula no está confirmada en el período indicado"));

    boolean justificada = Boolean.TRUE.equals(request.justificada());
    if (justificada == (request.nota() != null)) {
      throw new BusinessException(
          "Una evaluación justificada no debe tener nota y una evaluación sin justificar requiere"
              + " nota");
    }
    String evidenciaUrl = normalizarUrl(request.evidenciaUrl());
    boolean tieneArchivo = archivo != null && !archivo.isEmpty();
    if (tieneArchivo && evidenciaUrl != null) {
      throw new BusinessException("Adjunte un archivo o indique una URL, no ambos");
    }
    if (!tieneArchivo) {
      validarUrl(evidenciaUrl);
    }

    CalificacionParcial nota =
        notasParciales
            .findByEvaluacionIdAndMatriculaIdAndActiveTrue(evaluacionId, matriculaId)
            .orElseGet(CalificacionParcial::new);
    var notaAnterior = auditoriaCalificaciones.capturarParcial(nota);
    nota.setEvaluacion(evaluacion);
    nota.setMatricula(matricula);
    nota.setNota(request.nota());
    nota.setJustificada(justificada);
    if (tieneArchivo) {
      EvidenciaStorageService.ArchivoGuardado guardado = storage.guardar(archivo);
      nota.setEvidenciaUrl(null);
      nota.setEvidenciaArchivo(guardado.clave());
      nota.setEvidenciaNombre(guardado.nombre());
      nota.setEvidenciaTipo(guardado.tipo());
      nota.setEvidenciaTamanio(guardado.tamanio());
    } else {
      nota.setEvidenciaUrl(evidenciaUrl);
      nota.setEvidenciaArchivo(null);
      nota.setEvidenciaNombre(null);
      nota.setEvidenciaTipo(null);
      nota.setEvidenciaTamanio(null);
    }
    nota.setFechaRegistro(LocalDateTime.now());
    nota = notasParciales.save(nota);
    auditoriaCalificaciones.registrarParcial(
        username,
        administrador ? "ADMINISTRADOR" : "DOCENTE",
        "REGISTRAR_O_ACTUALIZAR",
        notaAnterior,
        nota);
    recalcularCompetencia(
        periodo,
        evaluacion.getCompetencia().getCodigo(),
        false,
        username,
        administrador ? "ADMINISTRADOR" : "DOCENTE",
        "RECALCULAR_NOTA_PARCIAL");
    return respuestaNotaParcial(matricula, nota);
  }

  @Transactional(readOnly = true)
  public Resource descargarEvidenciaParcial(
      Integer calificacionParcialId, String username, boolean administrador) {
    CalificacionParcial nota =
        notasParciales
            .findByIdAndActiveTrue(calificacionParcialId)
            .orElseThrow(() -> new BusinessException("Calificación parcial no encontrada"));
    exigirAccesoDocente(
        username,
        administrador,
        nota.getEvaluacion().getPeriodo(),
        nota.getEvaluacion().getCompetencia().getCodigo());
    if (nota.getEvidenciaArchivo() == null) {
      throw new BusinessException("La evidencia de esta calificación es una URL externa");
    }
    return storage.cargar(nota.getEvidenciaArchivo());
  }

  @Transactional
  public RecalculoLogrosResponse recalcular(RecalculoLogrosRequest request, String username) {
    int filtros =
        (request.periodoId() == null ? 0 : 1)
            + (request.gradoId() == null ? 0 : 1)
            + (request.seccionId() == null ? 0 : 1);
    if (filtros != 1) {
      throw new BusinessException("Indique exactamente uno de periodoId, gradoId o seccionId");
    }
    List<Periodo> seleccionados;
    if (request.periodoId() != null) {
      seleccionados = periodos.findAllByIdAndActiveTrue(request.periodoId());
    } else if (request.gradoId() != null) {
      seleccionados = periodos.findAllByGradoIdAndActiveTrue(request.gradoId());
    } else {
      seleccionados = periodos.findAllBySeccionIdAndActiveTrue(request.seccionId());
    }

    int procesados = 0;
    int actualizados = 0;
    for (Periodo seleccionado : seleccionados) {
      Periodo periodo = periodos.findByIdAndActiveTrueForUpdate(seleccionado.getId()).orElse(null);
      if (periodo == null || Boolean.TRUE.equals(periodo.getActasCerradas())) {
        continue;
      }
      procesados++;
      for (String codigo :
          evaluaciones.findDistinctCompetenciaCodigosByPeriodoId(periodo.getId())) {
        actualizados +=
            recalcularCompetencia(
                periodo, codigo, false, username, "ADMINISTRADOR", "RECALCULO_MANUAL");
      }
    }
    return new RecalculoLogrosResponse(procesados, actualizados);
  }

  @Transactional
  public CierreActasResponse cerrarActas(Integer periodoId, String username) {
    Periodo periodo =
        periodos
            .findByIdAndActiveTrueForUpdate(periodoId)
            .orElseThrow(() -> new BusinessException("Período no encontrado"));
    if (Boolean.TRUE.equals(periodo.getActasCerradas())) {
      throw new BusinessException("Las actas del período ya están cerradas");
    }
    for (String codigo : evaluaciones.findDistinctCompetenciaCodigosByPeriodoId(periodoId)) {
      recalcularCompetencia(periodo, codigo, true, username, "ADMINISTRADOR", "CIERRE_ACTAS");
    }
    LocalDateTime fechaCierre = LocalDateTime.now();
    periodo.setActasCerradas(true);
    periodo.setFechaCierreActas(fechaCierre);
    periodo.setUsuarioCierreActas(username);
    periodos.save(periodo);

    Integer idUsuario =
        usuarios.findByUsername(username).map(cuenta -> cuenta.getId()).orElse(null);
    auditoria.save(
        new RegistroAuditoria(
            idUsuario,
            "ADMINISTRADOR",
            AccionAuditoria.CERRAR_ACTAS,
            "Cierre de actas del período " + periodoId + " por " + username));
    return new CierreActasResponse(periodoId, true, fechaCierre, username);
  }

  @Transactional(readOnly = true)
  public CierreActasResponse consultarCierreActas(Integer periodoId) {
    Periodo periodo = periodo(periodoId);
    return new CierreActasResponse(
        periodoId,
        Boolean.TRUE.equals(periodo.getActasCerradas()),
        periodo.getFechaCierreActas(),
        periodo.getUsuarioCierreActas());
  }

  private int recalcularCompetencia(
      Periodo periodo,
      String codigo,
      boolean cerrar,
      String username,
      String rolUsuario,
      String accionAuditoria) {
    List<EvaluacionParcial> evaluacionesActivas =
        evaluaciones.findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrueOrderById(
            periodo.getId(), codigo);
    if (evaluacionesActivas.isEmpty()) {
      calificaciones
          .findAllByPeriodoIdAndCompetenciaCodigoAndActiveTrue(periodo.getId(), codigo)
          .stream()
          .filter(c -> Boolean.TRUE.equals(c.getConsolidada()))
          .forEach(
              c -> {
                var anterior = auditoriaCalificaciones.capturarFinal(c);
                c.setActive(false);
                calificaciones.save(c);
                auditoriaCalificaciones.registrarFinal(
                    username, rolUsuario, accionAuditoria, anterior, c);
              });
      return 0;
    }
    List<Matricula> roster =
        matriculas.findAllByPeriodoIdAndActiveTrueAndEstadoIn(
            periodo.getId(), List.of(EstadoMatricula.CONFIRMADA));
    List<Integer> idsEvaluaciones =
        evaluacionesActivas.stream().map(EvaluacionParcial::getId).toList();
    List<CalificacionParcial> existentes =
        notasParciales.findAllByEvaluacionIdInAndActiveTrue(idsEvaluaciones);
    Map<String, CalificacionParcial> porEvaluacionMatricula = new HashMap<>();
    for (CalificacionParcial nota : existentes) {
      porEvaluacionMatricula.put(
          clave(nota.getEvaluacion().getId(), nota.getMatricula().getId()), nota);
    }

    if (cerrar) {
      List<CalificacionParcial> completar = new ArrayList<>();
      Map<CalificacionParcial, AuditoriaCalificacionService.ParcialAnterior> anteriores =
          new java.util.IdentityHashMap<>();
      for (EvaluacionParcial evaluacion : evaluacionesActivas) {
        for (Matricula matricula : roster) {
          String key = clave(evaluacion.getId(), matricula.getId());
          CalificacionParcial nota = porEvaluacionMatricula.get(key);
          boolean cambio = false;
          if (nota == null) {
            nota = new CalificacionParcial();
            nota.setEvaluacion(evaluacion);
            nota.setMatricula(matricula);
            nota.setJustificada(false);
            nota.setFechaRegistro(LocalDateTime.now());
            porEvaluacionMatricula.put(key, nota);
            existentes.add(nota);
            cambio = true;
          }
          var notaAnterior = auditoriaCalificaciones.capturarParcial(nota);
          if (!Boolean.TRUE.equals(nota.getJustificada()) && nota.getNota() == null) {
            nota.setNota("C");
            cambio = true;
          }
          if (cambio) {
            anteriores.put(nota, notaAnterior);
            completar.add(nota);
          }
        }
      }
      List<CalificacionParcial> guardadas = notasParciales.saveAll(completar);
      for (CalificacionParcial nota : guardadas) {
        auditoriaCalificaciones.registrarParcial(
            username, rolUsuario, "COMPLETAR_FALTANTE_CIERRE_ACTAS", anteriores.get(nota), nota);
      }
    }

    ReglaConsolidacionResponse regla = obtenerRegla(codigo);
    Map<Integer, Calificacion> finalesExistentes = new HashMap<>();
    calificaciones
        .findAllByPeriodoIdAndCompetenciaCodigo(periodo.getId(), codigo)
        .forEach(c -> finalesExistentes.put(c.getMatricula().getId(), c));
    Map<Integer, List<CalificacionParcial>> porMatricula = new HashMap<>();
    for (CalificacionParcial nota : existentes) {
      if (!Boolean.TRUE.equals(nota.getActive())) {
        continue;
      }
      porMatricula
          .computeIfAbsent(nota.getMatricula().getId(), ignored -> new ArrayList<>())
          .add(nota);
    }

    int actualizados = 0;
    List<Calificacion> finalesAGuardar = new ArrayList<>();
    Map<Calificacion, AuditoriaCalificacionService.FinalAnterior> anterioresFinales =
        new java.util.IdentityHashMap<>();
    for (Matricula matricula : roster) {
      List<CalificacionParcial> notas = porMatricula.getOrDefault(matricula.getId(), List.of());
      ConsolidacionCalculada resultado = calcularResultado(notas, regla);
      Calificacion calificacionExistente = finalesExistentes.get(matricula.getId());
      if (resultado.puntajeConsolidado() == null) {
        if (calificacionExistente != null
            && Boolean.TRUE.equals(calificacionExistente.getActive())) {
          anterioresFinales.put(
              calificacionExistente, auditoriaCalificaciones.capturarFinal(calificacionExistente));
          calificacionExistente.setActive(false);
          finalesAGuardar.add(calificacionExistente);
        }
        continue;
      }
      Calificacion calificacion =
          calificacionExistente == null ? new Calificacion() : calificacionExistente;
      anterioresFinales.put(
          calificacion, auditoriaCalificaciones.capturarFinal(calificacionExistente));
      calificacion.setPeriodo(periodo);
      calificacion.setMatricula(matricula);
      calificacion.setCompetencia(buscarCompetencia(codigo));
      calificacion.setNota(resultado.nota());
      calificacion.setPuntajeConsolidado(resultado.puntajeConsolidado());
      calificacion.setConsolidada(true);
      calificacion.setObservacion(null);
      calificacion.setEvidenciaUrl(null);
      calificacion.setEvidenciaArchivo(null);
      calificacion.setEvidenciaNombre(null);
      calificacion.setEvidenciaTipo(null);
      calificacion.setEvidenciaTamanio(null);
      calificacion.setFechaRegistro(LocalDateTime.now());
      calificacion.setActive(true);
      finalesAGuardar.add(calificacion);
      actualizados++;
    }
    List<Calificacion> guardadas = calificaciones.saveAll(finalesAGuardar);
    for (Calificacion calificacion : guardadas) {
      auditoriaCalificaciones.registrarFinal(
          username, rolUsuario, accionAuditoria, anterioresFinales.get(calificacion), calificacion);
    }
    return actualizados;
  }

  private ReglaConsolidacionResponse respuestaRegla(ReglaConsolidacion regla) {
    return new ReglaConsolidacionResponse(
        regla.getCompetenciaCodigo(),
        VALOR_C,
        VALOR_B,
        VALOR_A,
        VALOR_AD,
        regla.getUmbralB(),
        regla.getUmbralA(),
        regla.getUmbralAd());
  }

  private EvaluacionParcialResponse respuestaEvaluacion(EvaluacionParcial evaluacion) {
    return new EvaluacionParcialResponse(
        evaluacion.getId(), evaluacion.getPeriodo().getId(),
        evaluacion.getCompetencia().getCodigo(), evaluacion.getTitulo(),
        evaluacion.getDescripcion(), evaluacion.getPonderacion(), evaluacion.getBimestre());
  }

  private CalificacionParcialResponse respuestaNotaParcial(
      Matricula matricula, CalificacionParcial nota) {
    return new CalificacionParcialResponse(
        nota == null ? null : nota.getId(),
        matricula.getId(),
        matricula.getEstudiante().getId(),
        matricula.getEstudiante().getPersona().getNombreCompleto(),
        nota == null ? null : nota.getNota(),
        nota != null && Boolean.TRUE.equals(nota.getJustificada()),
        nota == null
            ? null
            : nota.getEvidenciaArchivo() != null
                ? "ARCHIVO"
                : nota.getEvidenciaUrl() != null ? "URL" : null,
        nota == null ? null : nota.getEvidenciaNombre(),
        nota == null
            ? null
            : nota.getEvidenciaArchivo() == null
                ? nota.getEvidenciaUrl()
                : "/api/v1/evaluaciones-parciales/calificaciones/" + nota.getId() + "/archivo",
        nota == null ? null : nota.getEvidenciaTamanio(),
        nota == null ? null : nota.getFechaRegistro());
  }

  private Periodo periodo(Integer id) {
    return periodos
        .findById(id)
        .filter(Periodo::getActive)
        .orElseThrow(() -> new BusinessException("Período no encontrado"));
  }

  private Periodo periodoAbierto(Integer id) {
    Periodo periodo =
        periodos
            .findByIdAndActiveTrueForUpdate(id)
            .orElseThrow(() -> new BusinessException("Período no encontrado"));
    if (Boolean.TRUE.equals(periodo.getActasCerradas())) {
      throw new BusinessException(
          "No se pueden modificar calificaciones: las actas están cerradas");
    }
    return periodo;
  }

  private EvaluacionParcial evaluacionActiva(Integer id) {
    return evaluaciones
        .findById(id)
        .filter(EvaluacionParcial::getActive)
        .orElseThrow(() -> new BusinessException("Evaluación parcial no encontrada"));
  }

  private Competencia buscarCompetencia(String codigo) {
    return competencias
        .findById(codigo)
        .orElseThrow(() -> new BusinessException("Competencia no encontrada"));
  }

  private void validarCompetenciaActiva(Competencia competencia) {
    if (!Boolean.TRUE.equals(competencia.getActivo())) {
      throw new BusinessException("La competencia está inactiva");
    }
  }

  private void validarUmbrales(BigDecimal umbralB, BigDecimal umbralA, BigDecimal umbralAd) {
    if (umbralB.compareTo(umbralA) >= 0
        || umbralA.compareTo(umbralAd) >= 0
        || umbralAd.compareTo(VALOR_AD) > 0) {
      throw new BusinessException(
          "Los umbrales deben cumplir 1 < umbralB < umbralA < umbralAD <= 4");
    }
  }

  private BigDecimal valorOrdinal(String nota) {
    return switch (nota) {
      case "AD" -> VALOR_AD;
      case "A" -> VALOR_A;
      case "B" -> VALOR_B;
      case "C" -> VALOR_C;
      default -> throw new BusinessException("La nota parcial debe ser AD, A, B o C");
    };
  }

  private String convertirNota(BigDecimal puntaje, ReglaConsolidacionResponse regla) {
    if (puntaje.compareTo(regla.umbralAd()) >= 0) {
      return "AD";
    }
    if (puntaje.compareTo(regla.umbralA()) >= 0) {
      return "A";
    }
    if (puntaje.compareTo(regla.umbralB()) >= 0) {
      return "B";
    }
    return "C";
  }

  private ConsolidacionCalculada calcularResultado(
      List<CalificacionParcial> notas, ReglaConsolidacionResponse regla) {
    BigDecimal ponderacionTotal = BigDecimal.ZERO;
    BigDecimal sumaPonderada = BigDecimal.ZERO;
    for (CalificacionParcial nota : notas) {
      if (Boolean.TRUE.equals(nota.getJustificada()) || nota.getNota() == null) {
        continue;
      }
      BigDecimal ponderacion = nota.getEvaluacion().getPonderacion();
      ponderacionTotal = ponderacionTotal.add(ponderacion);
      sumaPonderada = sumaPonderada.add(valorOrdinal(nota.getNota()).multiply(ponderacion));
    }

    if (ponderacionTotal.signum() == 0) {
      return new ConsolidacionCalculada(null, null);
    }

    BigDecimal puntajeSinRedondear =
        sumaPonderada.divide(ponderacionTotal, 10, RoundingMode.HALF_UP);
    return new ConsolidacionCalculada(
        convertirNota(puntajeSinRedondear, regla), puntajeSinRedondear.setScale(4, RoundingMode.HALF_UP));
  }

  public record ConsolidacionCalculada(String nota, BigDecimal puntajeConsolidado) {}

  private String clave(Integer evaluacionId, Integer matriculaId) {
    return evaluacionId + ":" + matriculaId;
  }

  private String limpiar(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }

  private String normalizarUrl(String url) {
    return url == null || url.isBlank() ? null : url.trim();
  }

  private void validarUrl(String url) {
    if (url == null) {
      return;
    }
    try {
      URI uri = URI.create(url);
      if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          || uri.getHost() == null) {
        throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException e) {
      throw new BusinessException("La URL de evidencia debe usar HTTP o HTTPS");
    }
  }

  private void exigirAccesoDocente(
      String username, boolean administrador, Periodo periodo, String competenciaCodigo) {
    if (administrador) {
      return;
    }
    Docente docente = docentes.obtenerDeCuenta(username);
    boolean dictaCompetencia =
        asignacionesHorario
            .buscarAsignacionesDocente(
                docente.getPersona().getId(),
                periodo.getSeccion().getId(),
                periodo.getFechaInicio(),
                periodo.getFechaFin())
            .stream()
            .anyMatch(
                asignacion ->
                    asignacion.getArea().getCompetencias().stream()
                        .anyMatch(c -> c.getCodigo().equals(competenciaCodigo)));
    if (!dictaCompetencia) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Solo puedes consultar o calificar tus competencias asignadas");
    }
  }
}
