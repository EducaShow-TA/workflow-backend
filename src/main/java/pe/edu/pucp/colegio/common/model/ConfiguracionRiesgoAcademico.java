package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracion_riesgo_academico")
public class ConfiguracionRiesgoAcademico {

  @Id private Integer id;

  @Column(nullable = false)
  private Integer umbral;

  protected ConfiguracionRiesgoAcademico() {}

  public ConfiguracionRiesgoAcademico(Integer id, Integer umbral) {
    this.id = id;
    this.umbral = umbral;
  }

  public Integer getUmbral() {
    return umbral;
  }

  public void setUmbral(Integer umbral) {
    this.umbral = umbral;
  }
}
