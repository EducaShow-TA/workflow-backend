package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
    name = "asistencias_docente",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_asistencia_docente_fecha",
            columnNames = {"docente_id", "fecha"}))
public class AsistenciaDocente extends EntidadBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "docente_id", nullable = false)
  private Docente docente;

  @Column(nullable = false)
  private LocalDate fecha;

  @Column(name = "hora_entrada")
  private LocalTime horaEntrada;

  @Column(name = "hora_salida")
  private LocalTime horaSalida;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoAsistenciaDocente estado = EstadoAsistenciaDocente.PENDIENTE_MARCAJE;

  @Column(name = "motivo_ajuste", length = 500)
  private String motivoAjuste;

  public Docente getDocente() {
    return docente;
  }

  public void setDocente(Docente docente) {
    if (this.docente == docente) {
      return;
    }
    if (this.docente != null) {
      this.docente.getAsistencias().remove(this);
    }
    this.docente = docente;
    if (docente != null) {
      docente.getAsistencias().add(this);
    }
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public void setFecha(LocalDate fecha) {
    this.fecha = fecha;
  }

  public LocalTime getHoraEntrada() {
    return horaEntrada;
  }

  public LocalTime getHoraSalida() {
    return horaSalida;
  }

  public EstadoAsistenciaDocente getEstado() {
    return estado;
  }

  public String getMotivoAjuste() {
    return motivoAjuste;
  }

  public void marcarIngreso(LocalTime hora) {
    if (hora == null) {
      throw new IllegalArgumentException("La hora de ingreso es obligatoria");
    }
    horaEntrada = hora;
    estado = EstadoAsistenciaDocente.PRESENTE;
  }

  public void marcarSalida(LocalTime hora) {
    if (hora == null) {
      throw new IllegalArgumentException("La hora de salida es obligatoria");
    }
    if (horaEntrada != null && hora.isBefore(horaEntrada)) {
      throw new IllegalArgumentException("La salida no puede ser anterior al ingreso");
    }
    horaSalida = hora;
  }

  public void marcarInasistencia() {
    horaEntrada = null;
    horaSalida = null;
    estado = EstadoAsistenciaDocente.INASISTENCIA;
  }

  public void ajustar(EstadoAsistenciaDocente estado) {
    if (estado == null) {
      throw new IllegalArgumentException("El estado de asistencia es obligatorio");
    }
    this.estado = estado;
    motivoAjuste = null;
    if (estado == EstadoAsistenciaDocente.INASISTENCIA
        || estado == EstadoAsistenciaDocente.PENDIENTE_MARCAJE) {
      horaEntrada = null;
      horaSalida = null;
    }
  }

  public void ajustar(EstadoAsistenciaDocente estado, String motivo) {
    if (motivo == null || motivo.isBlank()) {
      throw new IllegalArgumentException("El motivo del ajuste es obligatorio");
    }
    ajustar(estado);
    motivoAjuste = motivo.trim();
  }
}
