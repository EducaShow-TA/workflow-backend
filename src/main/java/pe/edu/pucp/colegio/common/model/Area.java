package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "areas")
public class Area {

  @Id
  @Column(name = "codigo", nullable = false, length = 50)
  private String codigo;

  @Column(nullable = false)
  private String nombre;

  private String descripcion;

  @ManyToMany
  @JoinTable(
      name = "areas_competencias",
      joinColumns = @JoinColumn(name = "area_codigo"),
      inverseJoinColumns = @JoinColumn(name = "competencia_codigo"))
  private Set<Competencia> competencias = new LinkedHashSet<>();

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public Set<Competencia> getCompetencias() {
    return competencias;
  }

  public void setCompetencias(Set<Competencia> competencias) {
    this.competencias = competencias;
  }
}
