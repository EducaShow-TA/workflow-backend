package pe.edu.pucp.colegio.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.edu.pucp.colegio.common.model.OrigenEntregaTarea;

/** Tarea vista por el alumno, con el estado de su propia entrega (US-3.9 y US-3.10). */
public record TareaEstudianteResponse(
    Integer id,
    Integer periodoId,
    String areaCodigo,
    String area,
    String competenciaCodigo,
    String competencia,
    String docente,
    String titulo,
    String descripcion,
    LocalDateTime fechaLimite,
    BigDecimal ponderacion,
    Estado estado,
    LocalDateTime fechaEntrega,
    OrigenEntregaTarea origen,
    String archivoNombre,
    Long archivoTamanio,
    boolean puedeEntregar,
    boolean cuentaParaNota,
    Integer bimestre,
    String nota,
    boolean notaJustificada,
    String comentario,
    LocalDateTime fechaCalificacion) {

  public enum Estado {
    PENDIENTE,
    ENTREGADA,
    VENCIDA
  }
}
