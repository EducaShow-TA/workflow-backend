package pe.edu.pucp.colegio.common.dto;

public record EstudianteResponse(
    Integer estudianteId,
    Integer personaId,
    String username,
    String rol,
    String dni,
    String nombreCompleto) {}
