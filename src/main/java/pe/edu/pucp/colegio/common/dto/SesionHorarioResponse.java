package pe.edu.pucp.colegio.common.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record SesionHorarioResponse(
    Integer id,
    Integer bloqueId,
    DayOfWeek dia,
    String areaCodigo,
    String areaNombre,
    Integer docenteId,
    String docenteNombre,
    Integer aulaId,
    String aulaCodigo,
    LocalDate vigenteDesde,
    LocalDate vigenteHasta) {}
