package pe.edu.pucp.colegio.security.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import pe.edu.pucp.colegio.common.model.*;

@Entity
@Table(name = "cuentas_usuario")
public class CuentaUsuario extends EntidadBase {

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Rol rol;

  @Column(name = "intentos_fallidos")
  private Integer intentosFallidos = 0;

  @Column(name = "bloqueada_hasta")
  private LocalDateTime bloqueadaHasta;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "persona_id", unique = true)
  private Persona persona;

  protected CuentaUsuario() {}

  public CuentaUsuario(String username, String passwordHash, Rol rol, Persona persona) {
    this.username = username;
    this.password = passwordHash;
    this.rol = rol;
    this.persona = persona;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Rol getRol() {
    return rol;
  }

  public void setRol(Rol rol) {
    this.rol = rol;
  }

  public Integer getIntentosFallidos() {
    return intentosFallidos;
  }

  public Persona getPersona() {
    return persona;
  }

  public void setPersona(Persona persona) {
    this.persona = persona;
  }

  public boolean estaBloqueada() {
    return bloqueadaHasta != null && LocalDateTime.now().isBefore(bloqueadaHasta);
  }

  public void registrarIntentoFallido(int max, int minutes) {
    intentosFallidos = (intentosFallidos == null ? 0 : intentosFallidos) + 1;
    if (intentosFallidos >= max) {
      bloqueadaHasta = LocalDateTime.now().plusMinutes(minutes);
    }
  }

  public void reiniciarIntentos() {
    intentosFallidos = 0;
    bloqueadaHasta = null;
  }
}
