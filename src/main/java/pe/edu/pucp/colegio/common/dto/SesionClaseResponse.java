package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SesionClaseResponse(
    Integer id,
    Integer seccionId,
    String seccionCodigo,
    Integer gradoNumero,
    String areaCodigo,
    String areaNombre,
    Integer docenteId,
    String docenteNombre,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin) {}
