package pe.edu.pucp.colegio.seguridad.dto;

public record CuentaResponseDTO(
    Integer id, String username, String rol, String nombreCompleto, String dni) {}
