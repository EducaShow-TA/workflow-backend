package pe.edu.pucp.colegio.security.dto;

public record UsuarioPerfilDTO(
    Integer id, String username, String rol, String nombreCompleto, String dni) {}
