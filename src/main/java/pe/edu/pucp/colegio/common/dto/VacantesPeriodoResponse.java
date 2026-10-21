package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;

public record VacantesPeriodoResponse(
    Integer periodoId,
    Integer gradoId,
    Integer numeroGrado,
    String nivelEducativo,
    Integer seccionId,
    String seccion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    int vacantesTotales,
    long matriculasActivas,
    int vacantesDisponibles,
    boolean hayVacantes) {}
