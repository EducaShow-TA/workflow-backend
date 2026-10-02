package pe.edu.pucp.colegio.seguridad.service;

import pe.edu.pucp.colegio.seguridad.dto.*;

public interface AuthService {

  AuthResponseDTO login(LoginRequestDTO request);

  UsuarioPerfilDTO currentUser(String username);
}
