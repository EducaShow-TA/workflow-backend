package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "ventanas_matricula")
public class PeriodoMatricula extends EntidadBase {

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_limite", nullable = false)
  private LocalDate fechaLimite;

  public LocalDate getFechaInicio() {
    return fechaInicio;
  }

  public void setFechaInicio(LocalDate fechaInicio) {
    this.fechaInicio = fechaInicio;
  }

  public LocalDate getFechaLimite() {
    return fechaLimite;
  }

  public void setFechaLimite(LocalDate fechaLimite) {
    this.fechaLimite = fechaLimite;
  }
}
