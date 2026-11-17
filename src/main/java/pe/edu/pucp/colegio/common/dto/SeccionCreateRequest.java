package pe.edu.pucp.colegio.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import pe.edu.pucp.colegio.common.model.NivelEducativo;

public record SeccionCreateRequest(
    @NotNull @Min(1) Integer gradoNumero,
    @NotBlank @Pattern(regexp = "[A-Z]{1,2}") String nombre,
    @NotNull @Positive Integer vacantesTotales,
    NivelEducativo nivelEducativo) {

  public SeccionCreateRequest(Integer gradoNumero, String nombre, Integer vacantesTotales) {
    this(gradoNumero, nombre, vacantesTotales, null);
  }
}
