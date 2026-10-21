package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
    name = "grados",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_grado_nivel_numero",
            columnNames = {"nivel_educativo", "numero"}))
public class Grado extends EntidadBase {

  @Min(value = 1, message = "El número de grado debe ser mayor que cero")
  @Column(nullable = false)
  private Integer numero;

  @Enumerated(EnumType.STRING)
  @Column(name = "nivel_educativo", nullable = false, length = 20)
  private NivelEducativo nivelEducativo;

  @NotEmpty(message = "Un grado debe tener al menos una sección")
  @OneToMany(
      mappedBy = "grado",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private Set<Seccion> secciones = new LinkedHashSet<>();

  public Integer getNumero() {
    return numero;
  }

  public void setNumero(Integer numero) {
    this.numero = numero;
  }

  public NivelEducativo getNivelEducativo() {
    return nivelEducativo;
  }

  public void setNivelEducativo(NivelEducativo nivelEducativo) {
    this.nivelEducativo = nivelEducativo;
  }

  public Set<Seccion> getSecciones() {
    return secciones;
  }

  public void setSecciones(Set<Seccion> secciones) {
    new LinkedHashSet<>(this.secciones).forEach(seccion -> seccion.setGrado(null));
    this.secciones = new LinkedHashSet<>();
    if (secciones != null) {
      secciones.forEach(seccion -> seccion.setGrado(this));
    }
  }

  public void agregarSeccion(Seccion seccion) {
    if (seccion == null) {
      throw new IllegalArgumentException("La sección no puede ser nula");
    }
    seccion.setGrado(this);
  }
}
