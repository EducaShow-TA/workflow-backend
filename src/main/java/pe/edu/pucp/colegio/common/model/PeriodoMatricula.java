package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "ventanas_matricula")
public class PeriodoMatricula extends EntidadBase {

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_limite", nullable = false)
  private LocalDate fechaLimite;

  @Column(name = "anhio_escolar", nullable = false)
  private Integer anhioEscolar;

  @Column(name = "monto_matricula", nullable = false, precision = 12, scale = 2)
  private BigDecimal montoMatricula;

  @Column(name = "monto_pension", nullable = false, precision = 12, scale = 2)
  private BigDecimal montoPension;

  @PrePersist
  private void completarValoresAnuales() {
    if (anhioEscolar == null && fechaInicio != null) {
      anhioEscolar = fechaInicio.getYear();
    }
    if (montoMatricula == null) {
      montoMatricula = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    if (montoPension == null) {
      montoPension = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
  }

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

  public Integer getAnhioEscolar() {
    return anhioEscolar;
  }

  public void setAnhioEscolar(Integer anhioEscolar) {
    this.anhioEscolar = anhioEscolar;
  }

  public BigDecimal getMontoMatricula() {
    return montoMatricula;
  }

  public void setMontoMatricula(BigDecimal montoMatricula) {
    this.montoMatricula = montoMatricula;
  }

  public BigDecimal getMontoPension() {
    return montoPension;
  }

  public void setMontoPension(BigDecimal montoPension) {
    this.montoPension = montoPension;
  }
}
