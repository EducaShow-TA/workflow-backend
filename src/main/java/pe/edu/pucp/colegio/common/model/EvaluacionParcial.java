package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "evaluaciones_parciales")
public class EvaluacionParcial extends EntidadBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "periodo_id", nullable = false)
  private Periodo periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competencia_codigo", referencedColumnName = "codigo", nullable = false)
  private Competencia competencia;

  @Column(nullable = false, length = 180)
  private String titulo;

  @Column(length = 1000)
  private String descripcion;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal ponderacion;

  public Periodo getPeriodo() {
    return periodo;
  }

  public void setPeriodo(Periodo periodo) {
    this.periodo = periodo;
  }

  public Competencia getCompetencia() {
    return competencia;
  }

  public void setCompetencia(Competencia competencia) {
    this.competencia = competencia;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public BigDecimal getPonderacion() {
    return ponderacion;
  }

  public void setPonderacion(BigDecimal ponderacion) {
    this.ponderacion = ponderacion;
  }
}
