package pe.edu.pucp.colegio.security.service;

public interface RecuperacionPasswordService {

  void solicitarRecuperacion(String username);

  void restablecerPassword(String token, String nuevaPassword);
}
