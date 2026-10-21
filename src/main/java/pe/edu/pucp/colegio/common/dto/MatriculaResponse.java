package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;

public record MatriculaResponse(
    Integer matriculaId,
    Integer estudianteId,
    Integer periodoId,
    LocalDate fechaMatricula,
    EstadoMatricula estado) {}
