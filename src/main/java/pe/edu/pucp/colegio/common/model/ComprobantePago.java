package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "comprobantes_pago", uniqueConstraints = @UniqueConstraint(
    name = "uk_comprobante_numero", columnNames = "numero_comprobante"))
public class ComprobantePago extends EntidadBase {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "boleta_id", nullable = false)
  private BoletaPension boleta;
  @Column(name = "fecha_emision", nullable = false)
  private LocalDate fechaEmision;
  @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
  private BigDecimal montoPagado;
  @Enumerated(EnumType.STRING)
  @Column(name = "medio_pago", nullable = false, length = 20)
  private MedioPago medioPago;
  @Column(name = "numero_comprobante", nullable = false, length = 40)
  private String numeroComprobante;

  public BoletaPension getBoleta() { return boleta; }
  public void setBoleta(BoletaPension value) { boleta = value; }
  public LocalDate getFechaEmision() { return fechaEmision; }
  public void setFechaEmision(LocalDate value) { fechaEmision = value; }
  public BigDecimal getMontoPagado() { return montoPagado; }
  public void setMontoPagado(BigDecimal value) { montoPagado = value; }
  public MedioPago getMedioPago() { return medioPago; }
  public void setMedioPago(MedioPago value) { medioPago = value; }
  public String getNumeroComprobante() { return numeroComprobante; }
  public void setNumeroComprobante(String value) { numeroComprobante = value; }
  public BigDecimal calcularMontoFinal() { return montoPagado; }
}
