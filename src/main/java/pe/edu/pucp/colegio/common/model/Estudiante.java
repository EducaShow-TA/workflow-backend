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

  @OneToMany(mappedBy = "estudiante")
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
    vinculoFamiliar.setEstudiante(this);
    vinculosFamiliares.add(vinculoFamiliar);
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
