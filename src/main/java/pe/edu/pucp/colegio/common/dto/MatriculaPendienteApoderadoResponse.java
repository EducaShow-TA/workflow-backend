package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;

public record MatriculaPendienteApoderadoResponse(
    Integer estudianteId,
    String estudiante,
    Integer periodoId,
    int anhioEscolar,
    Integer gradoNumero,
    String seccion,
    BigDecimal montoMatricula,
    BigDecimal montoPension) {}
