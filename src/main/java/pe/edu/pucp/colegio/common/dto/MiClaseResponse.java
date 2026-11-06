package pe.edu.pucp.colegio.common.dto;

import java.util.List;

/** Clase del docente: un área en una sección, en el periodo académico vigente de esa sección. */
public record MiClaseResponse(
    Integer periodoId,
    Integer seccionId,
    Integer gradoNumero,
    String seccionCodigo,
    Integer anhio,
    String areaCodigo,
    String areaNombre,
    int totalAlumnos,
    List<CompetenciaClaseResponse> competencias) {}
