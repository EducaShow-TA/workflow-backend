package pe.edu.pucp.colegio.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametros_sistema")
public class ParametrosSistema {

  public static final int ID_UNICO = 1;

  @Id private Integer id;

  @Column(name = "desfase_segundos")
  private Long desfaseSegundos;

  @Column(name = "motivo_fecha", length = 300)
  private String motivoFecha;

  @Column(name = "fecha_simulada_por", length = 100)
  private String fechaSimuladaPor;

  @Column(name = "fecha_simulada_en")
  private LocalDateTime fechaSimuladaEn;

  @Column(name = "pago_en_linea_activo", nullable = false)
  private boolean pagoEnLineaActivo = true;

  protected ParametrosSistema() {}

  public ParametrosSistema(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public Long getDesfaseSegundos() {
    return desfaseSegundos;
  }

  public String getMotivoFecha() {
    return motivoFecha;
  }

  public String getFechaSimuladaPor() {
    return fechaSimuladaPor;
  }

  public LocalDateTime getFechaSimuladaEn() {
    return fechaSimuladaEn;
  }

  public boolean isPagoEnLineaActivo() {
    return pagoEnLineaActivo;
  }

  public void setPagoEnLineaActivo(boolean pagoEnLineaActivo) {
    this.pagoEnLineaActivo = pagoEnLineaActivo;
  }

  public void simularFecha(long desfase, String motivo, String usuario, LocalDateTime enHoraReal) {
    this.desfaseSegundos = desfase;
    this.motivoFecha = motivo;
    this.fechaSimuladaPor = usuario;
    this.fechaSimuladaEn = enHoraReal;
  }

  public void volverAFechaReal() {
    this.desfaseSegundos = null;
    this.motivoFecha = null;
    this.fechaSimuladaPor = null;
    this.fechaSimuladaEn = null;
  }
}
