package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "plantillas_horario",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_plantilla_horario_turno_nombre",
            columnNames = {"turno", "nombre"}))
public class PlantillaHorario extends EntidadBase {

  @Column(nullable = false, length = 100)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TurnoEscolar turno;

  @Column(name = "hora_inicio", nullable = false)
  private LocalTime horaInicio;

  @Column(name = "hora_fin", nullable = false)
  private LocalTime horaFin;

  @Column(name = "duracion_hora_pedagogica_minutos", nullable = false)
  private Integer duracionHoraPedagogicaMinutos;

  @Column(name = "cantidad_bloques_pedagogicos", nullable = false)
  private Integer cantidadBloquesPedagogicos;

  @OneToMany(mappedBy = "plantilla", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orden ASC")
  private List<BloqueHorario> bloques = new ArrayList<>();

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public TurnoEscolar getTurno() {
    return turno;
  }

  public void setTurno(TurnoEscolar turno) {
    this.turno = turno;
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

  public Integer getDuracionHoraPedagogicaMinutos() {
    return duracionHoraPedagogicaMinutos;
  }

  public void setDuracionHoraPedagogicaMinutos(Integer duracionHoraPedagogicaMinutos) {
    this.duracionHoraPedagogicaMinutos = duracionHoraPedagogicaMinutos;
  }

  public Integer getCantidadBloquesPedagogicos() {
    return cantidadBloquesPedagogicos;
  }

  public void setCantidadBloquesPedagogicos(Integer cantidadBloquesPedagogicos) {
    this.cantidadBloquesPedagogicos = cantidadBloquesPedagogicos;
  }

  public List<BloqueHorario> getBloques() {
    return bloques;
  }

  public void reemplazarBloques(List<BloqueHorario> nuevosBloques) {
    bloques.clear();
    nuevosBloques.forEach(
        bloque -> {
          bloque.setPlantilla(this);
          bloques.add(bloque);
        });
  }
}
