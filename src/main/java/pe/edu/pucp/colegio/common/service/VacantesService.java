package pe.edu.pucp.colegio.common.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.VacantesPeriodoResponse;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

/** Calcula y valida las vacantes disponibles para una sección. */
@Service
public class VacantesService {

  private static final LocalDate FECHA_MINIMA = LocalDate.of(1000, 1, 1);
  private static final LocalDate FECHA_MAXIMA = LocalDate.of(9999, 12, 31);

  private final PeriodoRepository periodos;
  private final MatriculaRepository matriculas;

  public VacantesService(PeriodoRepository periodos, MatriculaRepository matriculas) {
    this.periodos = periodos;
    this.matriculas = matriculas;
  }

  @Transactional(readOnly = true)
  public VacantesPeriodoResponse consultar(Integer periodoId) {
    Periodo periodo =
        periodos
            .findById(periodoId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el periodo indicado"));

    Integer vacantesTotales = periodo.getSeccion().getVacantesTotales();
    if (vacantesTotales == null || vacantesTotales < 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El total de vacantes de la sección no es válido");
    }
    return describir(periodo);
  }

  @Transactional(readOnly = true)
  public List<VacantesPeriodoResponse> listar(Integer anhio, Integer gradoId, Integer seccionId) {
    LocalDate desde = anhio == null ? FECHA_MINIMA : LocalDate.of(anhio, 1, 1);
    LocalDate hasta = anhio == null ? FECHA_MAXIMA : LocalDate.of(anhio, 12, 31);
    return periodos.buscar(desde, hasta, gradoId, seccionId).stream().map(this::describir).toList();
  }

  /**
   * Las matrículas ocupan vacantes de la sección durante todo el año del periodo, aunque la sección
   * tenga más de un periodo en ese año.
   */
  public VacantesPeriodoResponse describir(Periodo periodo) {
    Integer capacidad = periodo.getSeccion().getVacantesTotales();
    int vacantesTotales = capacidad == null || capacidad < 0 ? 0 : capacidad;
    long matriculasActivas =
        matriculas.countAlumnosActivosPorSeccionEnAnhio(
            periodo.getSeccion().getId(), periodo.getFechaInicio().getYear());
    int vacantesDisponibles = (int) Math.max(0, vacantesTotales - matriculasActivas);

    return new VacantesPeriodoResponse(
        periodo.getId(),
        periodo.getGrado().getId(),
        periodo.getGrado().getNumero(),
        periodo.getGrado().getNivelEducativo().name(),
        periodo.getSeccion().getId(),
        periodo.getSeccion().getCodigo(),
        periodo.getFechaInicio(),
        periodo.getFechaFin(),
        vacantesTotales,
        matriculasActivas,
        vacantesDisponibles,
        vacantesDisponibles > 0,
        periodo.calcularEstado(LocalDate.now()));
  }
}
