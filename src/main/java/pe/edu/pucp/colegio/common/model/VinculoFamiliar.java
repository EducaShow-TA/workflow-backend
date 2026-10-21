package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "vinculos_familiares",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_vinculo_familiar_estudiante", columnNames = "estudiante_id"))
public class VinculoFamiliar extends EntidadBase {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Parentesco parentesco;

  @Column(name = "es_principal", nullable = false)
  private boolean esPrincipal;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "apoderado_id", nullable = false)
  private Apoderado apoderado;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "estudiante_id", nullable = false, unique = true)
  private Estudiante estudiante;

  public Parentesco getParentesco() {
    return parentesco;
  }

  public void setParentesco(Parentesco parentesco) {
    this.parentesco = parentesco;
  }

  public boolean isEsPrincipal() {
    return esPrincipal;
  }

  public void setEsPrincipal(boolean esPrincipal) {
    this.esPrincipal = esPrincipal;
  }

  public Apoderado getApoderado() {
    return apoderado;
  }

  public void setApoderado(Apoderado apoderado) {
    if (this.apoderado == apoderado) {
      return;
    }
    if (this.apoderado != null) {
      this.apoderado.getVinculosFamiliares().remove(this);
    }
    this.apoderado = apoderado;
    if (apoderado != null) {
      apoderado.getVinculosFamiliares().add(this);
    }
  }

  public Estudiante getEstudiante() {
    return estudiante;
  }

  public void setEstudiante(Estudiante estudiante) {
    if (this.estudiante == estudiante) {
      return;
    }
    Estudiante anterior = this.estudiante;
    this.estudiante = estudiante;
    if (anterior != null && anterior.getVinculoFamiliar() == this) {
      anterior.setVinculoFamiliar(null);
    }
    if (estudiante != null && estudiante.getVinculoFamiliar() != this) {
      estudiante.setVinculoFamiliar(this);
    }
  }
}
