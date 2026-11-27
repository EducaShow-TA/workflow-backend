package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record CursoReporteAnualResponse(
    String codigo, String nombre, List<CompetenciaReporteAnualResponse> competencias) {}
