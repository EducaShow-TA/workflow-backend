package pe.edu.pucp.colegio.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequestDTO {

  @NotBlank(message = "El nombre de usuario o correo es obligatorio")
  private String username;

  @NotBlank(message = "La contraseña es obligatoria")
  private String password;

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
}
