package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.edu.pucp.colegio.common.model.EstadoPeriodoMatricula;

public record PeriodoMatriculaResponse(
    Integer id,
    LocalDate fechaInicio,
    LocalDate fechaLimite,
    EstadoPeriodoMatricula estado,
    boolean tieneMatriculas,
    int anhioEscolar,
    BigDecimal montoMatricula,
    BigDecimal montoPension) {}
