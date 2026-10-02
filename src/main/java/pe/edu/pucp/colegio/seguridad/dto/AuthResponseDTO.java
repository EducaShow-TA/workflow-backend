package pe.edu.pucp.colegio.seguridad.dto;

public record AuthResponseDTO(
    String token, String tokenType, long expiresIn, UsuarioPerfilDTO usuario) {}
