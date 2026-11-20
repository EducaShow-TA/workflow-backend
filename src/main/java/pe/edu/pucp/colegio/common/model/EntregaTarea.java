package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "entregas_tarea",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_entrega_tarea_matricula",
            columnNames = {"tarea_id", "matricula_id"}))
public class EntregaTarea extends EntidadBase {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tarea_id", nullable = false)
  private Tarea tarea;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matricula_id", nullable = false)
  private Matricula matricula;

  @Column(nullable = false)
  private Boolean cumplida = false;

  @Column(name = "fecha_cumplimiento")
  private LocalDateTime fechaCumplimiento;

  @Column(name = "archivo_clave", length = 500)
  private String archivoClave;

  @Column(name = "archivo_nombre")
  private String archivoNombre;

  @Column(name = "archivo_tipo", length = 150)
  private String archivoTipo;

  @Column(name = "archivo_tamanio")
  private Long archivoTamanio;

  @Column(name = "archivo_fecha")
  private LocalDateTime archivoFecha;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private OrigenEntregaTarea origen;

  public Tarea getTarea() {
    return tarea;
  }

  public void setTarea(Tarea tarea) {
    this.tarea = tarea;
  }

  public Matricula getMatricula() {
    return matricula;
  }

  public void setMatricula(Matricula matricula) {
    this.matricula = matricula;
  }

  public Boolean getCumplida() {
    return cumplida;
  }

  public void setCumplida(Boolean cumplida) {
    this.cumplida = cumplida;
  }

  public LocalDateTime getFechaCumplimiento() {
    return fechaCumplimiento;
  }

  public void setFechaCumplimiento(LocalDateTime fechaCumplimiento) {
    this.fechaCumplimiento = fechaCumplimiento;
  }

  public String getArchivoClave() {
    return archivoClave;
  }

  public String getArchivoNombre() {
    return archivoNombre;
  }

  public String getArchivoTipo() {
    return archivoTipo;
  }

  public Long getArchivoTamanio() {
    return archivoTamanio;
  }

  public LocalDateTime getArchivoFecha() {
    return archivoFecha;
  }

  public void adjuntarArchivo(
      String clave, String nombre, String tipo, long tamanio, LocalDateTime fecha) {
    this.archivoClave = clave;
    this.archivoNombre = nombre;
    this.archivoTipo = tipo;
    this.archivoTamanio = tamanio;
    this.archivoFecha = fecha;
  }

  public OrigenEntregaTarea getOrigen() {
    return origen;
  }

  public void setOrigen(OrigenEntregaTarea origen) {
    this.origen = origen;
  }
}
