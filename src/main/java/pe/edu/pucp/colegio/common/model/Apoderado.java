package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "apoderados")
public class Apoderado extends EntidadBase {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "persona_id", nullable = false, unique = true)
  private Persona persona;

  @OneToMany(mappedBy = "apoderado")
  private Set<VinculoFamiliar> vinculosFamiliares = new LinkedHashSet<>();

  public Persona getPersona() {
    return persona;
  }

  public void setPersona(Persona persona) {
    this.persona = persona;
  }

  public Set<VinculoFamiliar> getVinculosFamiliares() {
    return vinculosFamiliares;
  }

  public void agregarVinculoFamiliar(VinculoFamiliar vinculoFamiliar) {
    if (vinculoFamiliar == null) {
      throw new IllegalArgumentException("El vínculo familiar no puede ser nulo");
    }
    vinculoFamiliar.setApoderado(this);
  }
}
