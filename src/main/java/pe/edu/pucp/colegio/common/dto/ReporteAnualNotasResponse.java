package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record ReporteAnualNotasResponse(
    Integer estudianteId,
    String estudiante,
    Integer periodoId,
    int anhioEscolar,
    int ultimoBimestre,
    Integer gradoNumero,
    String seccion,
    boolean actasCerradas,
    List<CursoReporteAnualResponse> cursos) {}
