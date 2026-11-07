package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import pe.edu.pucp.colegio.common.model.EstadoPeriodo;

public record PeriodoResponse(
    Integer id,
    Integer gradoId,
    Integer seccionId,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    EstadoPeriodo estado) {}
