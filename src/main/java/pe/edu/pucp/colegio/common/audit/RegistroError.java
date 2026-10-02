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
}
