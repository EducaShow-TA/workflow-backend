package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "matriculas",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_matricula_estudiante_periodo",
            columnNames = {"estudiante_id", "periodo_id"}))
public class Matricula extends EntidadBase {

  @Column(name = "fecha_matricula", nullable = false)
  private LocalDate fechaMatricula;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoMatricula estado;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "periodo_id", nullable = false)
  private Periodo periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "estudiante_id", nullable = false)
  private Estudiante estudiante;

  public LocalDate getFechaMatricula() {
    return fechaMatricula;
  }

  public void setFechaMatricula(LocalDate fechaMatricula) {
    this.fechaMatricula = fechaMatricula;
  }

  public EstadoMatricula getEstado() {
    return estado;
  }

  public void setEstado(EstadoMatricula estado) {
    this.estado = estado;
  }

  public Periodo getPeriodo() {
    return periodo;
  }

  public void setPeriodo(Periodo periodo) {
    if (this.periodo == periodo) {
      return;
    }
    if (this.periodo != null) {
      this.periodo.getMatriculas().remove(this);
    }
    this.periodo = periodo;
    if (periodo != null) {
      periodo.getMatriculas().add(this);
    }
  }

  public Estudiante getEstudiante() {
    return estudiante;
  }

  public void setEstudiante(Estudiante estudiante) {
    if (this.estudiante == estudiante) {
      return;
    }
    if (this.estudiante != null) {
      this.estudiante.getMatriculas().remove(this);
    }
    this.estudiante = estudiante;
    if (estudiante != null) {
      estudiante.getMatriculas().add(this);
    }
  }
}
