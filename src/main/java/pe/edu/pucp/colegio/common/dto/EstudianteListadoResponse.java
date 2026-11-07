package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record EstudianteListadoResponse(
    Integer estudianteId,
    Integer personaId,
    String dni,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String correo,
    List<MatriculaResponse> matriculas) {}
