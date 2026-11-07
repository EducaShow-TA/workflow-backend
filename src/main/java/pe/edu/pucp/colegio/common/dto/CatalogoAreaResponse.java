package pe.edu.pucp.colegio.common.dto;

import java.util.List;
import java.util.Set;

public record CatalogoAreaResponse(
    Integer id,
    String codigo,
    String nombre,
    String descripcion,
    Boolean activo,
    Set<String> competenciaCodigos,
    List<CatalogoCompetenciaResponse> competencias) {}
