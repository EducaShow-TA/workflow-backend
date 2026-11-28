package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(
    name = "boletas_pension",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_boleta_compromiso_mes_tipo",
            columnNames = {"compromiso_id", "mes_cobro", "tipo_cargo"}))
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

  /** Recargo monetario por mora; es distinto del estado de pago EN_MORA. */
  @Column(name = "mora", nullable = false, precision = 12, scale = 2)
  private BigDecimal recargoMora = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_pago", nullable = false, length = 20)
  private EstadoBoleta estadoPago = EstadoBoleta.PENDIENTE;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_cargo", nullable = false, length = 20)
  private TipoCargoBoleta tipoCargo = TipoCargoBoleta.PENSION;

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

  public BigDecimal getRecargoMora() {
    return recargoMora;
  }

  public void setRecargoMora(BigDecimal value) {
    recargoMora = value;
  }

  /** Compatibilidad para usos existentes; el valor representa el recargo, no el estado de mora. */
  @Deprecated
  public BigDecimal getMora() {
    return getRecargoMora();
  }

  /** Compatibilidad para usos existentes; el valor representa el recargo, no el estado de mora. */
  @Deprecated
  public void setMora(BigDecimal value) {
    setRecargoMora(value);
  }

  public EstadoBoleta getEstadoPago() {
    return estadoPago;
  }

  public void setEstadoPago(EstadoBoleta value) {
    estadoPago = value;
  }

  public TipoCargoBoleta getTipoCargo() {
    return tipoCargo;
  }

  public void setTipoCargo(TipoCargoBoleta value) {
    tipoCargo = value;
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
    return monto.add(recargoMora);
  }
}
