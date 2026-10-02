package pe.edu.pucp.colegio.seguridad.model;

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

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Rol getRol() {
    return rol;
  }

  public Integer getIntentosFallidos() {
    return intentosFallidos;
  }

  public Persona getPersona() {
    return persona;
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
