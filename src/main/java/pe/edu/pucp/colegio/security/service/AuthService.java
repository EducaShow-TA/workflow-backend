package pe.edu.pucp.colegio.security.service;

import pe.edu.pucp.colegio.security.dto.AuthResponseDTO;
import pe.edu.pucp.colegio.security.dto.LoginRequestDTO;
import pe.edu.pucp.colegio.security.dto.UsuarioPerfilDTO;

public interface AuthService {

  AuthResponseDTO login(LoginRequestDTO request);

  UsuarioPerfilDTO currentUser(String username);
}
