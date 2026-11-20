package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.pucp.colegio.common.model.OrigenEntregaTarea;

public record TareaResponse(
    Integer id,
    Integer periodoId,
    String competenciaCodigo,
    String competencia,
    String titulo,
    String descripcion,
    LocalDateTime fechaLimite,
    BigDecimal ponderacion,
    List<EntregaResponse> entregas) {
  public record EntregaResponse(
      Integer matriculaId,
      Integer estudianteId,
      String estudiante,
      boolean cumplida,
      LocalDateTime fechaCumplimiento,
      OrigenEntregaTarea origen,
      String archivoNombre,
      Long archivoTamanio,
      LocalDateTime archivoFecha) {}
}
