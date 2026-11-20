package pe.edu.pucp.colegio.common.dto;

import java.util.List;

public record CatalogoLibretaSnapshot(
    String codigo, String nombre, List<Competencia> competencias) {
  public record Competencia(String codigo, String nombre) {}
}
