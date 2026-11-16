package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Horario semanal de una sección para un año escolar, construido sobre una plantilla de bloques.
 */
@Entity
@Table(
    name = "horarios_seccion",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_horario_seccion_anio",
            columnNames = {"seccion_id", "anio"}))
public class HorarioSeccion extends EntidadBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seccion_id", nullable = false)
  private Seccion seccion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plantilla_id", nullable = false)
  private PlantillaHorario plantilla;

  @Column(nullable = false)
  private Integer anio;

  public Seccion getSeccion() {
    return seccion;
  }

  public void setSeccion(Seccion seccion) {
    this.seccion = seccion;
  }

  public PlantillaHorario getPlantilla() {
    return plantilla;
  }

  public void setPlantilla(PlantillaHorario plantilla) {
    this.plantilla = plantilla;
  }

  public Integer getAnio() {
    return anio;
  }

  public void setAnio(Integer anio) {
    this.anio = anio;
  }
}
