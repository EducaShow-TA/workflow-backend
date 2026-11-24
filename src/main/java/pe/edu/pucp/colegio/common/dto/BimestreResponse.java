package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BimestreResponse(
    Integer anhio,
    Integer numero,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String estado,
    boolean configurado,
    LocalDateTime fechaCierre,
    String usuarioCierre) {}
