package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

/** Fecha sin clases, por ejemplo un feriado, receso o cierre institucional. */
@Entity
@Table(
    name = "dias_no_lectivos",
    uniqueConstraints = @UniqueConstraint(name = "uk_dia_no_lectivo_fecha", columnNames = "fecha"))
public class DiaNoLectivo extends EntidadBase {

  @Column(nullable = false)
  private LocalDate fecha;

  @Column(nullable = false, length = 120)
  private String descripcion;

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }
}
