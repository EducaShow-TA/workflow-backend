package pe.edu.pucp.colegio.security.dto;

public record CuentaCargadaDTO(
    int fila, String rol, String dni, String username, String contrasenhaTemporal) {}
