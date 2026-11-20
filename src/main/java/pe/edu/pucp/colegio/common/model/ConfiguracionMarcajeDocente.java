package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Configuración única que controla la ventana y la tolerancia del marcaje docente. */
@Entity
@Table(name = "configuracion_marcaje_docente")
public class ConfiguracionMarcajeDocente {

  @Id private Integer id;

  @Column(name = "tolerancia_minutos", nullable = false)
  private Integer toleranciaMinutos;

  @Column(name = "ventana_antes_minutos", nullable = false)
  private Integer ventanaAntesMinutos;

  @Column(name = "ventana_despues_minutos", nullable = false)
  private Integer ventanaDespuesMinutos;

  protected ConfiguracionMarcajeDocente() {}

  public ConfiguracionMarcajeDocente(
      Integer id, Integer toleranciaMinutos, Integer ventanaAntesMinutos, Integer ventanaDespuesMinutos) {
    this.id = id;
    this.toleranciaMinutos = toleranciaMinutos;
    this.ventanaAntesMinutos = ventanaAntesMinutos;
    this.ventanaDespuesMinutos = ventanaDespuesMinutos;
  }

  public Integer getToleranciaMinutos() {
    return toleranciaMinutos;
  }

  public void setToleranciaMinutos(Integer toleranciaMinutos) {
    this.toleranciaMinutos = toleranciaMinutos;
  }

  public Integer getVentanaAntesMinutos() {
    return ventanaAntesMinutos;
  }

  public void setVentanaAntesMinutos(Integer ventanaAntesMinutos) {
    this.ventanaAntesMinutos = ventanaAntesMinutos;
  }

  public Integer getVentanaDespuesMinutos() {
    return ventanaDespuesMinutos;
  }

  public void setVentanaDespuesMinutos(Integer ventanaDespuesMinutos) {
    this.ventanaDespuesMinutos = ventanaDespuesMinutos;
  }
}
