package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "entregas_tarea",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_entrega_tarea_matricula",
            columnNames = {"tarea_id", "matricula_id"}))
public class EntregaTarea extends EntidadBase {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tarea_id", nullable = false)
  private Tarea tarea;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matricula_id", nullable = false)
  private Matricula matricula;

  @Column(nullable = false)
  private Boolean cumplida = false;

  @Column(name = "fecha_cumplimiento")
  private LocalDateTime fechaCumplimiento;

  public Tarea getTarea() {
    return tarea;
  }

  public void setTarea(Tarea tarea) {
    this.tarea = tarea;
  }

  public Matricula getMatricula() {
    return matricula;
  }

  public void setMatricula(Matricula matricula) {
    this.matricula = matricula;
  }

  public Boolean getCumplida() {
    return cumplida;
  }

  public void setCumplida(Boolean cumplida) {
    this.cumplida = cumplida;
  }

  public LocalDateTime getFechaCumplimiento() {
    return fechaCumplimiento;
  }

  public void setFechaCumplimiento(LocalDateTime fechaCumplimiento) {
    this.fechaCumplimiento = fechaCumplimiento;
  }
}
