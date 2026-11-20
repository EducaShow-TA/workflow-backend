package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_horario")
public class AuditoriaHorario extends EntidadBase {

  @Column(name = "horario_id", nullable = false)
  private Integer horarioId;

  @Column(nullable = false, length = 120)
  private String seccion;

  @Column(nullable = false, length = 20)
  private String accion;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  @Column(name = "usuario_cuenta_id")
  private Integer usuarioCuentaId;

  @Column(name = "usuario_nombre", nullable = false, length = 160)
  private String usuarioNombre;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String detalle;

  @Column(name = "valores_anteriores", columnDefinition = "TEXT")
  private String valoresAnteriores;

  @Column(name = "valores_nuevos", columnDefinition = "TEXT")
  private String valoresNuevos;

  @Column(name = "vigencia_tipo", nullable = false, length = 20)
  private String vigenciaTipo;

  @Column(name = "vigente_desde")
  private LocalDate vigenteDesde;

  @Column(name = "vigente_hasta")
  private LocalDate vigenteHasta;

  public Integer getHorarioId() {
    return horarioId;
  }

  public void setHorarioId(Integer horarioId) {
    this.horarioId = horarioId;
  }

  public String getSeccion() {
    return seccion;
  }

  public void setSeccion(String seccion) {
    this.seccion = seccion;
  }

  public String getAccion() {
    return accion;
  }

  public void setAccion(String accion) {
    this.accion = accion;
  }

  public LocalDateTime getFechaHora() {
    return fechaHora;
  }

  public void setFechaHora(LocalDateTime fechaHora) {
    this.fechaHora = fechaHora;
  }

  public Integer getUsuarioCuentaId() {
    return usuarioCuentaId;
  }

  public void setUsuarioCuentaId(Integer usuarioCuentaId) {
    this.usuarioCuentaId = usuarioCuentaId;
  }

  public String getUsuarioNombre() {
    return usuarioNombre;
  }

  public void setUsuarioNombre(String usuarioNombre) {
    this.usuarioNombre = usuarioNombre;
  }

  public String getDetalle() {
    return detalle;
  }

  public void setDetalle(String detalle) {
    this.detalle = detalle;
  }

  public String getValoresAnteriores() {
    return valoresAnteriores;
  }

  public void setValoresAnteriores(String valoresAnteriores) {
    this.valoresAnteriores = valoresAnteriores;
  }

  public String getValoresNuevos() {
    return valoresNuevos;
  }

  public void setValoresNuevos(String valoresNuevos) {
    this.valoresNuevos = valoresNuevos;
  }

  public String getVigenciaTipo() {
    return vigenciaTipo;
  }

  public void setVigenciaTipo(String vigenciaTipo) {
    this.vigenciaTipo = vigenciaTipo;
  }

  public LocalDate getVigenteDesde() {
    return vigenteDesde;
  }

  public void setVigenteDesde(LocalDate vigenteDesde) {
    this.vigenteDesde = vigenteDesde;
  }

  public LocalDate getVigenteHasta() {
    return vigenteHasta;
  }

  public void setVigenteHasta(LocalDate vigenteHasta) {
    this.vigenteHasta = vigenteHasta;
  }
}
