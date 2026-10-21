package pe.edu.pucp.colegio.security.service;

import pe.edu.pucp.colegio.security.dto.*;

public interface AuthService {

  AuthResponseDTO login(LoginRequestDTO request);

  UsuarioPerfilDTO currentUser(String username);
}
