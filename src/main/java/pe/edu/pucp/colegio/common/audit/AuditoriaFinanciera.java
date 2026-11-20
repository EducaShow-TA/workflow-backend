package pe.edu.pucp.colegio.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Evento de dinero o de estado de una boleta, con los datos tal como estaban en ese momento. */
@Entity
@Table(name = "auditoria_finanzas")
public class AuditoriaFinanciera {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "fecha_hora", nullable = false)
  private LocalDateTime fechaHora;

  @Column(nullable = false, length = 100)
  private String usuario;

  @Column(name = "rol_usuario", nullable = false, length = 30)
  private String rolUsuario;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AccionAuditoriaFinanciera accion;

  @Column(name = "compromiso_id", nullable = false)
  private Integer compromisoId;

  @Column(name = "boleta_id")
  private Integer boletaId;

  @Column(name = "estudiante_id", nullable = false)
  private Integer estudianteId;

  @Column(name = "estudiante_nombre", nullable = false, length = 255)
  private String estudianteNombre;

  @Column(name = "estudiante_dni", nullable = false, length = 15)
  private String estudianteDni;

  @Column(name = "grado_id", nullable = false)
  private Integer gradoId;

  @Column(name = "grado_numero", nullable = false)
  private Integer gradoNumero;

  @Column(name = "seccion_id", nullable = false)
  private Integer seccionId;

  @Column(name = "seccion_codigo", nullable = false, length = 50)
  private String seccionCodigo;

  @Column(name = "apoderado_id", nullable = false)
  private Integer apoderadoId;

  @Column(name = "apoderado_nombre", nullable = false, length = 255)
  private String apoderadoNombre;

  @Column(length = 120)
  private String concepto;

  @Column(name = "mes_cobro")
  private LocalDate mesCobro;

  @Column(name = "estado_anterior", length = 20)
  private String estadoAnterior;

  @Column(name = "estado_nuevo", nullable = false, length = 20)
  private String estadoNuevo;

  @Column(precision = 12, scale = 2)
  private BigDecimal monto;

  @Column(name = "numero_cuotas")
  private Integer numeroCuotas;

  @Column(length = 20)
  private String mecanismo;

  @Column(name = "medio_pago", length = 20)
  private String medioPago;

  @Column(name = "numero_comprobante", length = 40)
  private String numeroComprobante;

  protected AuditoriaFinanciera() {}

  public AuditoriaFinanciera(
      LocalDateTime fechaHora,
      String usuario,
      String rolUsuario,
      AccionAuditoriaFinanciera accion,
      Contexto contexto,
      String estadoAnterior,
      String estadoNuevo) {
    this.fechaHora = fechaHora;
    this.usuario = usuario;
    this.rolUsuario = rolUsuario;
    this.accion = accion;
    this.compromisoId = contexto.compromisoId();
    this.estudianteId = contexto.estudianteId();
    this.estudianteNombre = contexto.estudianteNombre();
    this.estudianteDni = contexto.estudianteDni();
    this.gradoId = contexto.gradoId();
    this.gradoNumero = contexto.gradoNumero();
    this.seccionId = contexto.seccionId();
    this.seccionCodigo = contexto.seccionCodigo();
    this.apoderadoId = contexto.apoderadoId();
    this.apoderadoNombre = contexto.apoderadoNombre();
    this.concepto = contexto.concepto();
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
  }

  /** Datos del compromiso, el estudiante y su apoderado titular al momento del evento. */
  public record Contexto(
      Integer compromisoId,
      Integer estudianteId,
      String estudianteNombre,
      String estudianteDni,
      Integer gradoId,
      Integer gradoNumero,
      Integer seccionId,
      String seccionCodigo,
      Integer apoderadoId,
      String apoderadoNombre,
      String concepto) {}

  public AuditoriaFinanciera conBoleta(Integer boletaId, LocalDate mesCobro, BigDecimal monto) {
    this.boletaId = boletaId;
    this.mesCobro = mesCobro;
    this.monto = monto;
    return this;
  }

  public AuditoriaFinanciera conCuotas(BigDecimal montoTotal, Integer numeroCuotas) {
    this.monto = montoTotal;
    this.numeroCuotas = numeroCuotas;
    return this;
  }

  public AuditoriaFinanciera conMecanismo(String mecanismo) {
    this.mecanismo = mecanismo;
    return this;
  }

  public AuditoriaFinanciera conPago(String medioPago, String numeroComprobante) {
    this.medioPago = medioPago;
    this.numeroComprobante = numeroComprobante;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public LocalDateTime getFechaHora() {
    return fechaHora;
  }

  public String getUsuario() {
    return usuario;
  }

  public String getRolUsuario() {
    return rolUsuario;
  }

  public AccionAuditoriaFinanciera getAccion() {
    return accion;
  }

  public Integer getCompromisoId() {
    return compromisoId;
  }

  public Integer getBoletaId() {
    return boletaId;
  }

  public Integer getEstudianteId() {
    return estudianteId;
  }

  public String getEstudianteNombre() {
    return estudianteNombre;
  }

  public String getEstudianteDni() {
    return estudianteDni;
  }

  public Integer getGradoId() {
    return gradoId;
  }

  public Integer getGradoNumero() {
    return gradoNumero;
  }

  public Integer getSeccionId() {
    return seccionId;
  }

  public String getSeccionCodigo() {
    return seccionCodigo;
  }

  public Integer getApoderadoId() {
    return apoderadoId;
  }

  public String getApoderadoNombre() {
    return apoderadoNombre;
  }

  public String getConcepto() {
    return concepto;
  }

  public LocalDate getMesCobro() {
    return mesCobro;
  }

  public String getEstadoAnterior() {
    return estadoAnterior;
  }

  public String getEstadoNuevo() {
    return estadoNuevo;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public Integer getNumeroCuotas() {
    return numeroCuotas;
  }

  public String getMecanismo() {
    return mecanismo;
  }

  public String getMedioPago() {
    return medioPago;
  }

  public String getNumeroComprobante() {
    return numeroComprobante;
  }
}
