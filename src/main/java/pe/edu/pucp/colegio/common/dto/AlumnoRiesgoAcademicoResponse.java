package pe.edu.pucp.colegio.common.dto;

public record AlumnoRiesgoAcademicoResponse(
    Integer matriculaId,
    Integer estudianteId,
    String dni,
    String nombreCompleto,
    int notasC,
    boolean enRiesgo) {}
