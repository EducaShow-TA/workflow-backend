package pe.edu.pucp.colegio.security.dto;

public record AuthResponseDTO(
    String token, String tokenType, long expiresIn, UsuarioPerfilDTO usuario) {}
