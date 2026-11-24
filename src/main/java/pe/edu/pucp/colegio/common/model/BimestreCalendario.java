package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bimestres_academicos")
public class BimestreCalendario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private Integer anhio;

  @Column(nullable = false)
  private Integer numero;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDate fechaFin;

  @Column(nullable = false)
  private boolean cerrado;

  @Column(name = "fecha_cierre")
  private LocalDateTime fechaCierre;

  @Column(name = "usuario_cierre", length = 100)
  private String usuarioCierre;

  protected BimestreCalendario() {}

  public BimestreCalendario(
      Integer anhio, Integer numero, LocalDate fechaInicio, LocalDate fechaFin) {
    this.anhio = anhio;
    this.numero = numero;
    this.fechaInicio = fechaInicio;
    this.fechaFin = fechaFin;
  }

  public Integer getId() {
    return id;
  }

  public Integer getAnhio() {
    return anhio;
  }

  public Integer getNumero() {
    return numero;
  }

  public LocalDate getFechaInicio() {
    return fechaInicio;
  }

  public LocalDate getFechaFin() {
    return fechaFin;
  }

  public boolean isCerrado() {
    return cerrado;
  }

  public LocalDateTime getFechaCierre() {
    return fechaCierre;
  }

  public String getUsuarioCierre() {
    return usuarioCierre;
  }

  public void cambiarFechas(LocalDate inicio, LocalDate fin) {
    this.fechaInicio = inicio;
    this.fechaFin = fin;
  }

  public void cerrar(LocalDateTime fecha, String usuario) {
    this.cerrado = true;
    this.fechaCierre = fecha;
    this.usuarioCierre = usuario;
  }
}
