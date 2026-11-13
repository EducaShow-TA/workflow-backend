package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "calificaciones",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_calificacion_periodo_matricula_competencia",
            columnNames = {"periodo_id", "matricula_id", "competencia_codigo"}))
public class Calificacion extends EntidadBase {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "periodo_id", nullable = false)
  private Periodo periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matricula_id", nullable = false)
  private Matricula matricula;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "competencia_codigo", referencedColumnName = "codigo", nullable = false)
  private Competencia competencia;

  @Column(nullable = false, length = 2)
  private String nota;

  @Column(name = "puntaje_consolidado", precision = 6, scale = 4)
  private java.math.BigDecimal puntajeConsolidado;

  @Column(nullable = false)
  private Boolean consolidada = false;

  @Column(length = 1000)
  private String observacion;

  @Column(name = "evidencia_url", length = 2048)
  private String evidenciaUrl;

  @Column(name = "evidencia_archivo", length = 500)
  private String evidenciaArchivo;

  @Column(name = "evidencia_nombre")
  private String evidenciaNombre;

  @Column(name = "evidencia_tipo", length = 150)
  private String evidenciaTipo;

  @Column(name = "evidencia_tamanio")
  private Long evidenciaTamanio;

  @Column(name = "fecha_registro", nullable = false)
  private LocalDateTime fechaRegistro;

  public Periodo getPeriodo() {
    return periodo;
  }

  public void setPeriodo(Periodo periodo) {
    this.periodo = periodo;
  }

  public Matricula getMatricula() {
    return matricula;
  }

  public void setMatricula(Matricula matricula) {
    this.matricula = matricula;
  }

  public Competencia getCompetencia() {
    return competencia;
  }

  public void setCompetencia(Competencia competencia) {
    this.competencia = competencia;
  }

  public String getNota() {
    return nota;
  }

  public void setNota(String nota) {
    this.nota = nota;
  }

  public java.math.BigDecimal getPuntajeConsolidado() {
    return puntajeConsolidado;
  }

  public void setPuntajeConsolidado(java.math.BigDecimal puntajeConsolidado) {
    this.puntajeConsolidado = puntajeConsolidado;
  }

  public Boolean getConsolidada() {
    return consolidada;
  }

  public void setConsolidada(Boolean consolidada) {
    this.consolidada = consolidada;
  }

  public String getObservacion() {
    return observacion;
  }

  public void setObservacion(String observacion) {
    this.observacion = observacion;
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
