package pe.edu.pucp.colegio.security.service;

import java.util.List;
import pe.edu.pucp.colegio.security.dto.ActualizarCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CambiarEstadoCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CambiarRolCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CrearCuentaRequestDTO;
import pe.edu.pucp.colegio.security.dto.CuentaResponseDTO;

/** Gestión de cuentas de usuario (US-5.2): alta y consulta, reservada al administrador. */
public interface CuentaUsuarioService {

  CuentaResponseDTO crear(CrearCuentaRequestDTO request, String usernameResponsable);

  CuentaResponseDTO actualizar(
      Integer id, ActualizarCuentaRequestDTO request, String usernameResponsable);

  CuentaResponseDTO cambiarEstado(
      Integer id, CambiarEstadoCuentaRequestDTO request, String usernameResponsable);

  CuentaResponseDTO cambiarRol(
      Integer id, CambiarRolCuentaRequestDTO request, String usernameResponsable);

  List<CuentaResponseDTO> listar();
}
