package pe.edu.pucp.colegio.security.dto;

public record CuentaResponseDTO(
    Integer id, String username, String rol, String nombreCompleto, String dni) {}
