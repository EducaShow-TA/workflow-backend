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
import java.time.LocalTime;

@Entity
@Table(
    name = "bloques_horario",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_bloque_horario_plantilla_orden",
            columnNames = {"plantilla_id", "orden"}))
public class BloqueHorario {

  @jakarta.persistence.Id
  @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plantilla_id", nullable = false)
  private PlantillaHorario plantilla;

  @Column(nullable = false)
  private Integer orden;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoBloqueHorario tipo;

  @Column(name = "hora_inicio", nullable = false)
  private LocalTime horaInicio;

  @Column(name = "hora_fin", nullable = false)
  private LocalTime horaFin;

  public Integer getId() {
    return id;
  }

  public PlantillaHorario getPlantilla() {
    return plantilla;
  }

  public void setPlantilla(PlantillaHorario plantilla) {
    this.plantilla = plantilla;
  }

  public Integer getOrden() {
    return orden;
  }

  public void setOrden(Integer orden) {
    this.orden = orden;
  }

  public TipoBloqueHorario getTipo() {
    return tipo;
  }

  public void setTipo(TipoBloqueHorario tipo) {
    this.tipo = tipo;
  }

  public LocalTime getHoraInicio() {
    return horaInicio;
  }

  public void setHoraInicio(LocalTime horaInicio) {
    this.horaInicio = horaInicio;
  }

  public LocalTime getHoraFin() {
    return horaFin;
  }

  public void setHoraFin(LocalTime horaFin) {
    this.horaFin = horaFin;
  }
}
