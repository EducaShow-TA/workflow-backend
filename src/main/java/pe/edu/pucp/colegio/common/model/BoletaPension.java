package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(
    name = "boletas_pension",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_boleta_compromiso_mes",
            columnNames = {"compromiso_id", "mes_cobro"}))
public class BoletaPension extends EntidadBase {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "compromiso_id", nullable = false)
  private CompromisoPago compromiso;

  @Column(name = "mes_cobro", nullable = false)
  private LocalDate mesCobro;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal monto;

  @Column(name = "fecha_vencimiento", nullable = false)
  private LocalDate fechaVencimiento;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal mora = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_pago", nullable = false, length = 20)
  private EstadoBoleta estadoPago = EstadoBoleta.PENDIENTE;

  @Enumerated(EnumType.STRING)
  @Column(name = "mecanismo_generacion", nullable = false, length = 20)
  private MecanismoGeneracion mecanismoGeneracion;

  @Column(name = "fecha_generacion", nullable = false)
  private LocalDateTime fechaGeneracion;

  public CompromisoPago getCompromiso() {
    return compromiso;
  }

  public void setCompromiso(CompromisoPago value) {
    compromiso = value;
  }

  public YearMonth getMesCobro() {
    return mesCobro == null ? null : YearMonth.from(mesCobro);
  }

  public void setMesCobro(YearMonth value) {
    mesCobro = value.atDay(1);
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public void setMonto(BigDecimal value) {
    monto = value;
  }

  public LocalDate getFechaVencimiento() {
    return fechaVencimiento;
  }

  public void setFechaVencimiento(LocalDate value) {
    fechaVencimiento = value;
  }

  public BigDecimal getMora() {
    return mora;
  }

  public void setMora(BigDecimal value) {
    mora = value;
  }

  public EstadoBoleta getEstadoPago() {
    return estadoPago;
  }

  public void setEstadoPago(EstadoBoleta value) {
    estadoPago = value;
  }

  public MecanismoGeneracion getMecanismoGeneracion() {
    return mecanismoGeneracion;
  }

  public void setMecanismoGeneracion(MecanismoGeneracion value) {
    mecanismoGeneracion = value;
  }

  public LocalDateTime getFechaGeneracion() {
    return fechaGeneracion;
  }

  public void setFechaGeneracion(LocalDateTime value) {
    fechaGeneracion = value;
  }

  public boolean estaProximaAVencer(LocalDate hoy) {
    return estadoPago == EstadoBoleta.PENDIENTE
        && !hoy.isAfter(fechaVencimiento)
        && !fechaVencimiento.isAfter(hoy.plusDays(5));
  }

  public void marcarEnMora(LocalDate hoy) {
    if (estadoPago == EstadoBoleta.PENDIENTE && hoy.isAfter(fechaVencimiento)) {
      estadoPago = EstadoBoleta.EN_MORA;
    }
  }

  public BigDecimal calcularMontoTotal() {
    return monto.add(mora);
  }
}
