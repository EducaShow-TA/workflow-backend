package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "reglas_consolidacion")
public class ReglaConsolidacion {

  @Id
  @Column(name = "competencia_codigo", nullable = false, length = 50)
  private String competenciaCodigo;

  @OneToOne
  @JoinColumn(
      name = "competencia_codigo",
      referencedColumnName = "codigo",
      insertable = false,
      updatable = false)
  private Competencia competencia;

  @Column(name = "umbral_b", nullable = false, precision = 4, scale = 2)
  private BigDecimal umbralB;

  @Column(name = "umbral_a", nullable = false, precision = 4, scale = 2)
  private BigDecimal umbralA;

  @Column(name = "umbral_ad", nullable = false, precision = 4, scale = 2)
  private BigDecimal umbralAd;

  public String getCompetenciaCodigo() {
    return competenciaCodigo;
  }

  public void setCompetenciaCodigo(String competenciaCodigo) {
    this.competenciaCodigo = competenciaCodigo;
  }

  public Competencia getCompetencia() {
    return competencia;
  }

  public BigDecimal getUmbralB() {
    return umbralB;
  }

  public void setUmbralB(BigDecimal umbralB) {
    this.umbralB = umbralB;
  }

  public BigDecimal getUmbralA() {
    return umbralA;
  }

  public void setUmbralA(BigDecimal umbralA) {
    this.umbralA = umbralA;
  }

  public BigDecimal getUmbralAd() {
    return umbralAd;
  }

  public void setUmbralAd(BigDecimal umbralAd) {
    this.umbralAd = umbralAd;
  }
}
