package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record CompetenciaReporteAnualResponse(
    String codigo, String nombre, List<String> notasPorBimestre) {}
