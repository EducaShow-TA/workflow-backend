package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "compromisos_pago")
public class CompromisoPago extends EntidadBase {
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matricula_id", nullable = false, unique = true)
  private Matricula matricula;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "apoderado_id", nullable = false)
  private Apoderado apoderado;
  @Column(nullable = false, length = 120)
  private String concepto;
  @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
  private BigDecimal montoTotal;
  @Column(name = "fecha_emision", nullable = false)
  private LocalDate fechaEmision;
  @Column(name = "mes_inicio", nullable = false)
  private LocalDate mesInicio;
  @Column(name = "numero_cuotas", nullable = false)
  private Integer numeroCuotas;
  @Column(name = "primer_vencimiento", nullable = false)
  private LocalDate primerVencimiento;
  @Column(name = "dia_vencimiento", nullable = false)
  private Integer diaVencimiento;
  public Matricula getMatricula() { return matricula; }
  public void setMatricula(Matricula value) { matricula = value; }
  public Apoderado getApoderado() { return apoderado; }
  public void setApoderado(Apoderado value) { apoderado = value; }
  public String getConcepto() { return concepto; }
  public void setConcepto(String value) { concepto = value; }
  public BigDecimal getMontoTotal() { return montoTotal; }
  public void setMontoTotal(BigDecimal value) { montoTotal = value; }
  public LocalDate getFechaEmision() { return fechaEmision; }
  public void setFechaEmision(LocalDate value) { fechaEmision = value; }
  public YearMonth getMesInicio() { return mesInicio == null ? null : YearMonth.from(mesInicio); }
  public void setMesInicio(YearMonth value) { mesInicio = value.atDay(1); }
  public Integer getNumeroCuotas() { return numeroCuotas; }
  public void setNumeroCuotas(Integer value) { numeroCuotas = value; }
  public LocalDate getPrimerVencimiento() { return primerVencimiento; }
  public void setPrimerVencimiento(LocalDate value) { primerVencimiento = value; }
  public Integer getDiaVencimiento() { return diaVencimiento; }
  public void setDiaVencimiento(Integer value) { diaVencimiento = value; }

  public BigDecimal getMontoCuota(int numeroCuota) {
    BigDecimal base = montoTotal.divide(BigDecimal.valueOf(numeroCuotas), 2, java.math.RoundingMode.DOWN);
    return numeroCuota == numeroCuotas
        ? montoTotal.subtract(base.multiply(BigDecimal.valueOf(numeroCuotas - 1L)))
        : base;
  }

  public BigDecimal getMontoCuota() {
    return montoTotal.divide(BigDecimal.valueOf(numeroCuotas), 2, java.math.RoundingMode.HALF_UP);
  }
}
