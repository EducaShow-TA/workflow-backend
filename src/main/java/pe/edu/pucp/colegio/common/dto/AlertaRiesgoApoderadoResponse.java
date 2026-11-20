package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record AlertaRiesgoApoderadoResponse(
    Integer estudianteId,
    Integer periodoId,
    int bimestre,
    String areaCodigo,
    String areaNombre,
    int notasC,
    int umbral,
    List<CompetenciaEnRiesgo> competencias) {
  public record CompetenciaEnRiesgo(String codigo, String nombre, int notasC) {}
}
