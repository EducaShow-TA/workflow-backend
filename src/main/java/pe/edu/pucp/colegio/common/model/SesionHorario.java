package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;

/** Celda de la grilla: qué área dicta qué docente, en qué aula, en un día y bloque. */
@Entity
@Table(
    name = "sesiones_horario",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_sesion_horario_bloque_dia",
            columnNames = {"horario_id", "bloque_id", "dia"}))
public class SesionHorario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "horario_id", nullable = false)
  private HorarioSeccion horario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "bloque_id", nullable = false)
  private BloqueHorario bloque;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 15)
  private DayOfWeek dia;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "area_codigo", nullable = false)
  private Area area;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "docente_id", nullable = false)
  private CuentaUsuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "aula_id", nullable = false)
  private Aula aula;

  public Integer getId() {
    return id;
  }

  public HorarioSeccion getHorario() {
    return horario;
  }

  public void setHorario(HorarioSeccion horario) {
    this.horario = horario;
  }

  public BloqueHorario getBloque() {
    return bloque;
  }

  public void setBloque(BloqueHorario bloque) {
    this.bloque = bloque;
  }

  public DayOfWeek getDia() {
    return dia;
  }

  public void setDia(DayOfWeek dia) {
    this.dia = dia;
  }

  public Area getArea() {
    return area;
  }

  public void setArea(Area area) {
    this.area = area;
  }

  public CuentaUsuario getDocente() {
    return docente;
  }

  public void setDocente(CuentaUsuario docente) {
    this.docente = docente;
  }

  public Aula getAula() {
    return aula;
  }

  public void setAula(Aula aula) {
    this.aula = aula;
  }
}
