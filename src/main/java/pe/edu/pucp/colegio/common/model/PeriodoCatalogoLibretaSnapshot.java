package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "periodo_catalogo_libreta_snapshot",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_periodo_catalogo_libreta_snapshot",
            columnNames = {"periodo_id", "area_codigo", "competencia_codigo"}))
public class PeriodoCatalogoLibretaSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "periodo_id", nullable = false)
  private Periodo periodo;

  @Column(name = "area_codigo", nullable = false, length = 50)
  private String areaCodigo;

  @Column(name = "area_nombre", nullable = false, length = 255)
  private String areaNombre;

  @Column(name = "competencia_codigo", nullable = false, length = 50)
  private String competenciaCodigo;

  @Column(name = "competencia_nombre", nullable = false, length = 255)
  private String competenciaNombre;

  protected PeriodoCatalogoLibretaSnapshot() {}

  public PeriodoCatalogoLibretaSnapshot(
      Periodo periodo,
      String areaCodigo,
      String areaNombre,
      String competenciaCodigo,
      String competenciaNombre) {
    this.periodo = periodo;
    this.areaCodigo = areaCodigo;
    this.areaNombre = areaNombre;
    this.competenciaCodigo = competenciaCodigo;
    this.competenciaNombre = competenciaNombre;
  }

  public Integer getId() {
    return id;
  }

  public Periodo getPeriodo() {
    return periodo;
  }

  public String getAreaCodigo() {
    return areaCodigo;
  }

  public String getAreaNombre() {
    return areaNombre;
  }

  public String getCompetenciaCodigo() {
    return competenciaCodigo;
  }

  public String getCompetenciaNombre() {
    return competenciaNombre;
  }
}
