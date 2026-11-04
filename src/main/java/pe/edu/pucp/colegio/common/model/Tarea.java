package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tareas")
public class Tarea extends EntidadBase {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "periodo_id", nullable = false)
  private Periodo periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competencia_codigo", referencedColumnName = "codigo", nullable = false)
  private Competencia competencia;

  @Column(nullable = false, length = 180)
  private String titulo;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String descripcion;

  @Column(name = "fecha_limite", nullable = false)
  private LocalDateTime fechaLimite;

  @Column(precision = 5, scale = 2)
  private BigDecimal ponderacion;

  public Periodo getPeriodo() { return periodo; }
  public void setPeriodo(Periodo periodo) { this.periodo = periodo; }
  public Competencia getCompetencia() { return competencia; }
  public void setCompetencia(Competencia competencia) { this.competencia = competencia; }
  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }
  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
  public LocalDateTime getFechaLimite() { return fechaLimite; }
  public void setFechaLimite(LocalDateTime fechaLimite) { this.fechaLimite = fechaLimite; }
  public BigDecimal getPonderacion() { return ponderacion; }
  public void setPonderacion(BigDecimal ponderacion) { this.ponderacion = ponderacion; }
}
