package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record RiesgoAcademicoResponse(
    Integer periodoId,
    int bimestre,
    String areaCodigo,
    int umbral,
    int totalAlumnos,
    int alumnosEnRiesgo,
    List<AlumnoRiesgoAcademicoResponse> alumnos) {}
