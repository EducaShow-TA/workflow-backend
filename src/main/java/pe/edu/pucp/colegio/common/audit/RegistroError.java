package pe.edu.pucp.colegio.common.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registros_error")
public class RegistroError {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "fecha_hora")
  private LocalDateTime fechaHora = LocalDateTime.now();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NivelLog nivel;

  @Column(name = "codigo_error")
  private String codigoError;

  @Column(name = "metodo_http")
  private String metodoHttp;

  private String endpoint;

  @Column(name = "codigo_estado")
  private Integer codigoEstado;

  private String mensaje;
  
  @Column(name = "id_usuario")
  private Integer idUsuario;

  private String modulo;

  @Column(name = "stack_trace", length = 4000)
  private String stackTrace;

  protected RegistroError() {}

  public RegistroError(
      NivelLog nivel,
      String codigoError,
      String metodoHttp,
      String endpoint,
      Integer codigoEstado,
      String mensaje) {
    this.nivel = nivel;
    this.codigoError = codigoError;
    this.metodoHttp = metodoHttp;
    this.endpoint = endpoint;
    this.codigoEstado = codigoEstado;
    this.mensaje = mensaje;
  }
  
  public Integer getId() { return id; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public NivelLog getNivel() { return nivel; }
    public String getCodigoError() { return codigoError; }
    public String getMetodoHttp() { return metodoHttp; }
    public String getEndpoint() { return endpoint; }
    public Integer getCodigoEstado() { return codigoEstado; }
    public String getMensaje() { return mensaje; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}
