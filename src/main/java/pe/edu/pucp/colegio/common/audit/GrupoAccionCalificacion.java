package pe.edu.pucp.colegio.common.audit;

import java.util.List;

public enum GrupoAccionCalificacion {
  RECTIFICACIONES(List.of(AccionesCalificacion.RECTIFICAR)),
  REGISTROS(List.of(AccionesCalificacion.REGISTRAR_O_ACTUALIZAR)),
  AUTOMATICAS(
      List.of(
          "RECALCULAR_REGLA",
          "RECALCULAR_EVALUACION",
          "RECALCULAR_NOTA_PARCIAL",
          "RECALCULO_MANUAL",
          "CIERRE_ACTAS",
          "DESACTIVAR_EVALUACION",
          "COMPLETAR_FALTANTE_CIERRE_ACTAS",
          "CIERRE_BIMESTRE",
          "COMPLETAR_FALTANTE_CIERRE_BIMESTRE"));

  private final List<String> acciones;

  GrupoAccionCalificacion(List<String> acciones) {
    this.acciones = acciones;
  }

  public List<String> acciones() {
    return acciones;
  }
}
