package pe.edu.pucp.colegio.security.dto;

public record CuentaResponseDTO(
    Integer id,
    String username,
    String correo,
    String rol,
    String nombreCompleto,
    String dni,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    Boolean activo) {}
