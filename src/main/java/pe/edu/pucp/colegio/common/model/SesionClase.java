package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
    name = "sesiones_clase",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_sesion_clase_seccion_area_fecha_inicio",
            columnNames = {"seccion_id", "area_codigo", "fecha", "hora_inicio"}))
public class SesionClase extends EntidadBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seccion_id", nullable = false)
  private Seccion seccion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "area_codigo", nullable = false)
  private Area area;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "docente_id", nullable = false)
  private Docente docente;

  /** Asignación semanal de origen; queda nula para sesiones creadas manualmente. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sesion_horario_id")
  private SesionHorario sesionHorario;

  @Column(nullable = false)
  private LocalDate fecha;

  @Column(name = "hora_inicio", nullable = false)
  private LocalTime horaInicio;

  @Column(name = "hora_fin", nullable = false)
  private LocalTime horaFin;

  @Column(nullable = false)
  private Boolean cancelada = false;

  @Column(name = "motivo_cancelacion", length = 300)
  private String motivoCancelacion;

  @OneToMany(mappedBy = "sesionClase")
  private Set<AsistenciaEstudiante> asistenciasEstudiantes = new LinkedHashSet<>();

  public Seccion getSeccion() {
    return seccion;
  }

  public void setSeccion(Seccion seccion) {
    this.seccion = seccion;
  }

  public Area getArea() {
    return area;
  }

  public void setArea(Area area) {
    this.area = area;
  }

  public Docente getDocente() {
    return docente;
  }

  public void setDocente(Docente docente) {
    if (this.docente == docente) {
      return;
    }
    if (this.docente != null) {
      this.docente.getSesionesClase().remove(this);
    }
    this.docente = docente;
    if (docente != null) {
      docente.getSesionesClase().add(this);
    }
  }

  public SesionHorario getSesionHorario() {
    return sesionHorario;
  }

  public void setSesionHorario(SesionHorario sesionHorario) {
    this.sesionHorario = sesionHorario;
  }

  public Boolean getCancelada() {
    return cancelada;
  }

  public void setCancelada(Boolean cancelada) {
    this.cancelada = cancelada;
  }

  public String getMotivoCancelacion() {
    return motivoCancelacion;
  }

  public void setMotivoCancelacion(String motivoCancelacion) {
    this.motivoCancelacion = motivoCancelacion;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public LocalTime getHoraInicio() {
    return horaInicio;
  }

  public void setHoraInicio(LocalTime horaInicio) {
    if (horaFin != null && horaInicio != null && !horaFin.isAfter(horaInicio)) {
      throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
    }
    this.horaInicio = horaInicio;
  }

  public LocalTime getHoraFin() {
    return horaFin;
  }

  public void setHoraFin(LocalTime horaFin) {
    if (horaInicio != null && horaFin != null && !horaFin.isAfter(horaInicio)) {
      throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
    }
    this.horaFin = horaFin;
  }

  public Set<AsistenciaEstudiante> getAsistenciasEstudiantes() {
    return asistenciasEstudiantes;
  }

  public void agregarAsistenciaEstudiante(AsistenciaEstudiante asistencia) {
    if (asistencia == null) {
      throw new IllegalArgumentException("La asistencia del estudiante no puede ser nula");
    }
    asistencia.setSesionClase(this);
  }
}
