package pe.edu.pucp.colegio.common.dto;

import java.time.DayOfWeek;

public record SesionClaseResponse(
    Integer id,
    Integer bloqueId,
    DayOfWeek dia,
    String areaCodigo,
    String areaNombre,
    Integer docenteId,
    String docenteNombre,
    Integer aulaId,
    String aulaCodigo) {}
