package pe.edu.pucp.colegio.common.dto;

import java.util.Set;

public record CatalogoCompetenciaResponse(
    Integer id,
    Integer areaId,
    String codigo,
    String nombre,
    String descripcion,
    Boolean activo,
    boolean tieneNotasRegistradas,
    Set<String> areaCodigos) {}
