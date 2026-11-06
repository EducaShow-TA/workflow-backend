package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "calificaciones_parciales",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_calificacion_parcial_evaluacion_matricula",
            columnNames = {"evaluacion_id", "matricula_id"}))
public class CalificacionParcial extends EntidadBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "evaluacion_id", nullable = false)
  private EvaluacionParcial evaluacion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matricula_id", nullable = false)
  private Matricula matricula;

  @Column(length = 2)
  private String nota;

  @Column(nullable = false)
  private Boolean justificada = false;

  @Column(name = "evidencia_url", length = 2048)
  private String evidenciaUrl;

  @Column(name = "evidencia_archivo", length = 500)
  private String evidenciaArchivo;

  @Column(name = "evidencia_nombre", length = 255)
  private String evidenciaNombre;

  @Column(name = "evidencia_tipo", length = 150)
  private String evidenciaTipo;

  @Column(name = "evidencia_tamanio")
  private Long evidenciaTamanio;

  @Column(name = "fecha_registro", nullable = false)
  private LocalDateTime fechaRegistro;

  public EvaluacionParcial getEvaluacion() {
    return evaluacion;
  }

  public void setEvaluacion(EvaluacionParcial evaluacion) {
    this.evaluacion = evaluacion;
  }

  public Matricula getMatricula() {
    return matricula;
  }

  public void setMatricula(Matricula matricula) {
    this.matricula = matricula;
  }

  public String getNota() {
    return nota;
  }

  public void setNota(String nota) {
    this.nota = nota;
  }

  public Boolean getJustificada() {
    return justificada;
  }

  public void setJustificada(Boolean justificada) {
    this.justificada = justificada;
  }

  public String getEvidenciaUrl() {
    return evidenciaUrl;
  }

  public void setEvidenciaUrl(String evidenciaUrl) {
    this.evidenciaUrl = evidenciaUrl;
  }

  public String getEvidenciaArchivo() {
    return evidenciaArchivo;
  }

  public void setEvidenciaArchivo(String evidenciaArchivo) {
    this.evidenciaArchivo = evidenciaArchivo;
  }

  public String getEvidenciaNombre() {
    return evidenciaNombre;
  }

  public void setEvidenciaNombre(String evidenciaNombre) {
    this.evidenciaNombre = evidenciaNombre;
  }

  public String getEvidenciaTipo() {
    return evidenciaTipo;
  }

  public void setEvidenciaTipo(String evidenciaTipo) {
    this.evidenciaTipo = evidenciaTipo;
  }

  public Long getEvidenciaTamanio() {
    return evidenciaTamanio;
  }

  public void setEvidenciaTamanio(Long evidenciaTamanio) {
    this.evidenciaTamanio = evidenciaTamanio;
  }

  public LocalDateTime getFechaRegistro() {
    return fechaRegistro;
  }

  public void setFechaRegistro(LocalDateTime fechaRegistro) {
    this.fechaRegistro = fechaRegistro;
  }
}
