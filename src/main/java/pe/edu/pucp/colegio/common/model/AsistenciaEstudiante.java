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
import java.time.LocalDateTime;

@Entity
@Table(
    name = "asistencias_estudiante",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_asistencia_estudiante_sesion",
            columnNames = {"sesion_clase_id", "estudiante_id"}))
public class AsistenciaEstudiante extends EntidadBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "estudiante_id", nullable = false)
  private Estudiante estudiante;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sesion_clase_id", nullable = false)
  private SesionClase sesionClase;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_asistencia", nullable = false, length = 20)
  private EstadoAsistencia estadoAsistencia = EstadoAsistencia.PENDIENTE_MARCAJE;

  @Column(name = "motivo_justificacion", length = 500)
  private String motivoJustificacion;

  @Column(name = "archivo_justificacion", length = 500)
  private String archivoJustificacion;

  @Column(name = "documento_justificacion_nombre", length = 255)
  private String documentoJustificacionNombre;

  @Column(name = "documento_justificacion_tipo", length = 150)
  private String documentoJustificacionTipo;

  @Column(name = "documento_justificacion_tamanio")
  private Long documentoJustificacionTamanio;

  @Column(name = "registrada_en")
  private LocalDateTime registradaEn;

  @Column(name = "registrada_por", length = 100)
  private String registradaPor;

  @Column(name = "corregida_en")
  private LocalDateTime corregidaEn;

  @Column(name = "corregida_por", length = 100)
  private String corregidaPor;

  public Estudiante getEstudiante() {
    return estudiante;
  }

  public void setEstudiante(Estudiante estudiante) {
    if (this.estudiante == estudiante) {
      return;
    }
    if (this.estudiante != null) {
      this.estudiante.getAsistencias().remove(this);
    }
    this.estudiante = estudiante;
    if (estudiante != null) {
      estudiante.getAsistencias().add(this);
    }
  }

  public SesionClase getSesionClase() {
    return sesionClase;
  }

  public void setSesionClase(SesionClase sesionClase) {
    if (this.sesionClase == sesionClase) {
      return;
    }
    if (this.sesionClase != null) {
      this.sesionClase.getAsistenciasEstudiantes().remove(this);
    }
    this.sesionClase = sesionClase;
    if (sesionClase != null) {
      sesionClase.getAsistenciasEstudiantes().add(this);
    }
  }

  public EstadoAsistencia getEstadoAsistencia() {
    return estadoAsistencia;
  }

  public void setEstadoAsistencia(EstadoAsistencia estadoAsistencia) {
    if (estadoAsistencia == null) {
      throw new IllegalArgumentException("El estado de asistencia es obligatorio");
    }
    this.estadoAsistencia = estadoAsistencia;
  }

  public String getMotivoJustificacion() {
    return motivoJustificacion;
  }

  public String getArchivoJustificacion() {
    return archivoJustificacion;
  }

  public String getDocumentoJustificacionNombre() {
    return documentoJustificacionNombre;
  }

  public String getDocumentoJustificacionTipo() {
    return documentoJustificacionTipo;
  }

  public Long getDocumentoJustificacionTamanio() {
    return documentoJustificacionTamanio;
  }

  public void setDocumentoJustificacion(String clave, String nombre, String tipo, Long tamanio) {
    archivoJustificacion = clave;
    documentoJustificacionNombre = nombre;
    documentoJustificacionTipo = tipo;
    documentoJustificacionTamanio = tamanio;
  }

  public void marcarPresente() {
    estadoAsistencia = EstadoAsistencia.PRESENTE;
    limpiarJustificacion();
  }

  public void marcarTardanza() {
    estadoAsistencia = EstadoAsistencia.TARDANZA;
    limpiarJustificacion();
  }

  public void marcarAusente() {
    estadoAsistencia = EstadoAsistencia.AUSENTE;
    limpiarJustificacion();
  }

  public void marcarPendiente() {
    estadoAsistencia = EstadoAsistencia.PENDIENTE_MARCAJE;
    limpiarJustificacion();
  }

  public void justificar(String motivo, String archivoJustificacion) {
    if (motivo == null || motivo.isBlank()) {
      throw new IllegalArgumentException("El motivo de justificación es obligatorio");
    }
    estadoAsistencia = EstadoAsistencia.JUSTIFICADA;
    motivoJustificacion = motivo.trim();
    this.archivoJustificacion =
        archivoJustificacion == null || archivoJustificacion.isBlank()
            ? null
            : archivoJustificacion.trim();
  }

  public LocalDateTime getRegistradaEn() {
    return registradaEn;
  }

  public String getRegistradaPor() {
    return registradaPor;
  }

  public LocalDateTime getCorregidaEn() {
    return corregidaEn;
  }

  public String getCorregidaPor() {
    return corregidaPor;
  }

  public void anotarRegistro(String usuario, LocalDateTime momento) {
    registradaPor = usuario;
    registradaEn = momento;
  }

  public void anotarCorreccion(String usuario, LocalDateTime momento) {
    corregidaPor = usuario;
    corregidaEn = momento;
  }

  private void limpiarJustificacion() {
    motivoJustificacion = null;
    archivoJustificacion = null;
    documentoJustificacionNombre = null;
    documentoJustificacionTipo = null;
    documentoJustificacionTamanio = null;
  }
}
