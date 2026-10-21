package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "estudiantes")
public class Estudiante extends EntidadBase {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "persona_id", nullable = false, unique = true)
  private Persona persona;

  @OneToMany(mappedBy = "estudiante")
  private Set<Matricula> matriculas = new LinkedHashSet<>();

  @OneToOne(mappedBy = "estudiante", fetch = FetchType.LAZY)
  private VinculoFamiliar vinculoFamiliar;

  public Persona getPersona() {
    return persona;
  }

  public void setPersona(Persona persona) {
    this.persona = persona;
  }

  public VinculoFamiliar getVinculoFamiliar() {
    return vinculoFamiliar;
  }

  public void setVinculoFamiliar(VinculoFamiliar vinculoFamiliar) {
    if (this.vinculoFamiliar == vinculoFamiliar) {
      return;
    }
    VinculoFamiliar anterior = this.vinculoFamiliar;
    this.vinculoFamiliar = vinculoFamiliar;
    if (anterior != null && anterior.getEstudiante() == this) {
      anterior.setEstudiante(null);
    }
    if (vinculoFamiliar != null && vinculoFamiliar.getEstudiante() != this) {
      vinculoFamiliar.setEstudiante(this);
    }
  }

  public Set<Matricula> getMatriculas() {
    return matriculas;
  }

  public void agregarMatricula(Matricula matricula) {
    if (matricula == null) {
      throw new IllegalArgumentException("La matrícula no puede ser nula");
    }
    matriculas.add(matricula);
    matricula.setEstudiante(this);
  }
}
