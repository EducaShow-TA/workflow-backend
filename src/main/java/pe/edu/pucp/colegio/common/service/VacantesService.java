package pe.edu.pucp.colegio.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.VacantesPeriodoResponse;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

@Service
public class VacantesService {

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
    long matriculasActivas = matriculas.countAlumnosActivosPorSeccion(periodo.getSeccion().getId());
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
        vacantesDisponibles > 0);
  }
}
