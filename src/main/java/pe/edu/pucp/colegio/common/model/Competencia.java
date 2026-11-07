package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "competencias")
public class Competencia {

  @Id
  @Column(name = "codigo", nullable = false, length = 50)
  private String codigo;

  @Column(name = "id", insertable = false, updatable = false)
  private Integer idCatalogo;

  @Column(name = "active", nullable = false)
  private Boolean activo = true;

  @Column(nullable = false)
  private String nombre;

  @Column(length = 500)
  private String descripcion;

  @ManyToMany(mappedBy = "competencias")
  private Set<Area> areas = new LinkedHashSet<>();

  public String getCodigo() {
    return codigo;
  }

  public Integer getIdCatalogo() {
    return idCatalogo;
  }

  public Boolean getActivo() {
    return activo;
  }

  public void setActivo(Boolean activo) {
    this.activo = activo;
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

  public Set<Area> getAreas() {
    return areas;
  }

  public void setAreas(Set<Area> areas) {
    this.areas = areas;
  }
}
