package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "secciones",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_seccion_grado_codigo", columnNames = {"grado_id", "codigo"}))
public class Seccion extends EntidadBase {

  @NotBlank(message = "El código de sección es obligatorio")
  @Size(max = 50)
  @Column(nullable = false, length = 50)
  private String codigo;

  @Column(name = "vacantes_totales", length = 20)
  private String vacantesTotales;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "grado_id", nullable = false)
  private Grado grado;

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getVacantesTotales() {
    return vacantesTotales;
  }

  public void setVacantesTotales(String vacantesTotales) {
    this.vacantesTotales = vacantesTotales;
  }

  public Grado getGrado() {
    return grado;
  }

  public void setGrado(Grado grado) {
    if (this.grado == grado) {
      return;
    }
    if (this.grado != null) {
      this.grado.getSecciones().remove(this);
    }
    this.grado = grado;
    if (grado != null) {
      grado.getSecciones().add(this);
    }
  }
}
