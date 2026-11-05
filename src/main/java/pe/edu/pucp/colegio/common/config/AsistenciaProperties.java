package pe.edu.pucp.colegio.common.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.asistencia")
public class AsistenciaProperties {

  @Min(0)
  private int ventanaMarcadoAntesMinutos = 15;

  @Min(0)
  private int ventanaMarcadoDespuesMinutos = 30;

  @Min(0)
  private int toleranciaDocenteMinutos = 10;

  public int getVentanaMarcadoAntesMinutos() {
    return ventanaMarcadoAntesMinutos;
  }

  public void setVentanaMarcadoAntesMinutos(int ventanaMarcadoAntesMinutos) {
    this.ventanaMarcadoAntesMinutos = ventanaMarcadoAntesMinutos;
  }

  public int getVentanaMarcadoDespuesMinutos() {
    return ventanaMarcadoDespuesMinutos;
  }

  public void setVentanaMarcadoDespuesMinutos(int ventanaMarcadoDespuesMinutos) {
    this.ventanaMarcadoDespuesMinutos = ventanaMarcadoDespuesMinutos;
  }

  public int getToleranciaDocenteMinutos() {
    return toleranciaDocenteMinutos;
  }

  public void setToleranciaDocenteMinutos(int toleranciaDocenteMinutos) {
    this.toleranciaDocenteMinutos = toleranciaDocenteMinutos;
  }
}
