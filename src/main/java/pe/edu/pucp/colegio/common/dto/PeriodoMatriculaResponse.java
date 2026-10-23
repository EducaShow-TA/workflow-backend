package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import pe.edu.pucp.colegio.common.model.EstadoPeriodoMatricula;

public record PeriodoMatriculaResponse(
    Integer id, LocalDate fechaInicio, LocalDate fechaLimite, EstadoPeriodoMatricula estado) {}
