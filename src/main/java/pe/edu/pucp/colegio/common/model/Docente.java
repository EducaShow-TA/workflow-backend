package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "docentes")
public class Docente extends EntidadBase {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "persona_id", nullable = false, unique = true)
  private Persona persona;

  @OneToMany(mappedBy = "docente")
  private Set<AsistenciaDocente> asistencias = new LinkedHashSet<>();

  @OneToMany(mappedBy = "docente")
  private Set<SesionClase> sesionesClase = new LinkedHashSet<>();

  public Persona getPersona() {
    return persona;
  }

  public void setPersona(Persona persona) {
    this.persona = persona;
  }

  public Set<AsistenciaDocente> getAsistencias() {
    return asistencias;
  }

  public void agregarAsistencia(AsistenciaDocente asistencia) {
    if (asistencia == null) {
      throw new IllegalArgumentException("La asistencia del docente no puede ser nula");
    }
    asistencia.setDocente(this);
  }

  public Set<SesionClase> getSesionesClase() {
    return sesionesClase;
  }
}
