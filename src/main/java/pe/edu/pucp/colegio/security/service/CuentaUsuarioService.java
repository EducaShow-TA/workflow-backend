package pe.edu.pucp.colegio.security.service;

import java.util.List;
import pe.edu.pucp.colegio.security.dto.*;

/** Gestión de cuentas de usuario (US-5.2): alta y consulta, reservada al administrador. */
public interface CuentaUsuarioService {

  CuentaResponseDTO crear(CrearCuentaRequestDTO request, String usernameResponsable);

  List<CuentaResponseDTO> listar();
}
