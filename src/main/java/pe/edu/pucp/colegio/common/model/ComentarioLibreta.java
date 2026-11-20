package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios_libreta")
public class ComentarioLibreta extends EntidadBase {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matricula_id", nullable = false, unique = true)
  private Matricula matricula;

  @Column(nullable = false, length = 2000)
  private String comentario;

  @Column(name = "autor_cuenta_id", nullable = false)
  private Integer autorCuentaId;

  @Column(name = "autor_nombre", nullable = false, length = 200)
  private String autorNombre;

  @Column(name = "actualizado_en", nullable = false)
  private LocalDateTime actualizadoEn;

  public Matricula getMatricula() {
    return matricula;
  }

  public void setMatricula(Matricula value) {
    matricula = value;
  }

  public String getComentario() {
    return comentario;
  }

  public void setComentario(String value) {
    comentario = value;
  }

  public Integer getAutorCuentaId() {
    return autorCuentaId;
  }

  public void setAutorCuentaId(Integer value) {
    autorCuentaId = value;
  }

  public String getAutorNombre() {
    return autorNombre;
  }

  public void setAutorNombre(String value) {
    autorNombre = value;
  }

  public LocalDateTime getActualizadoEn() {
    return actualizadoEn;
  }

  public void setActualizadoEn(LocalDateTime value) {
    actualizadoEn = value;
  }
}
