package pe.edu.pucp.colegio.common.dto;

import java.util.Set;

public record AreaResponse(
    String codigo, String nombre, String descripcion, Set<String> competenciaCodigos) {}
