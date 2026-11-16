package pe.edu.pucp.colegio.common.dto;

public record AulaResponse(
    Integer id, String codigo, Integer capacidad, String descripcion, Boolean activo) {}
