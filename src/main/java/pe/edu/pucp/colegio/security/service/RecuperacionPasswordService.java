package pe.edu.pucp.colegio.security.service;

public interface RecuperacionPasswordService {

  void solicitarRecuperacion(String correo);

  void restablecerPassword(String token, String nuevaPassword);
}
