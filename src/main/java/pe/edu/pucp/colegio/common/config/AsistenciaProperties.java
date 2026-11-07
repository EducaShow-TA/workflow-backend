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

  /** Minutos antes de la primera sesión del día desde los que el docente puede marcar su entrada. */
  @Min(0)
  private int ventanaDocenteAntesMinutos = 30;

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

  public int getVentanaDocenteAntesMinutos() {
    return ventanaDocenteAntesMinutos;
  }

  public void setVentanaDocenteAntesMinutos(int ventanaDocenteAntesMinutos) {
    this.ventanaDocenteAntesMinutos = ventanaDocenteAntesMinutos;
  }
}
