package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class EntidadBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private Boolean active = true;

  public Integer getId() {
    return id;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
