package pe.edu.pucp.colegio.common.dto;

public record SeccionResponse(
    Integer id,
    Integer gradoNumero,
    String nombre,
    int vacantesTotales,
    long cantidadMatriculados) {}
