package pe.edu.pucp.colegio.security.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import pe.edu.pucp.colegio.common.model.EntidadBase;

@Entity
@Table(name = "tokens_recuperacion_password")
public class TokenRecuperacionPassword extends EntidadBase {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cuenta_usuario_id", nullable = false)
  private CuentaUsuario cuentaUsuario;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "creado_en", nullable = false)
  private LocalDateTime creadoEn;

  @Column(name = "expira_en", nullable = false)
  private LocalDateTime expiraEn;

  @Column(name = "usado_en")
  private LocalDateTime usadoEn;

  protected TokenRecuperacionPassword() {}

  public TokenRecuperacionPassword(
      CuentaUsuario cuentaUsuario,
      String tokenHash,
      LocalDateTime creadoEn,
      LocalDateTime expiraEn) {
    this.cuentaUsuario = cuentaUsuario;
    this.tokenHash = tokenHash;
    this.creadoEn = creadoEn;
    this.expiraEn = expiraEn;
  }

  public CuentaUsuario getCuentaUsuario() {
    return cuentaUsuario;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public LocalDateTime getCreadoEn() {
    return creadoEn;
  }

  public LocalDateTime getExpiraEn() {
    return expiraEn;
  }

  public LocalDateTime getUsadoEn() {
    return usadoEn;
  }

  public void setUsadoEn(LocalDateTime usadoEn) {
    this.usadoEn = usadoEn;
  }
}
