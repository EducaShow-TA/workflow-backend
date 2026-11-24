package pe.edu.pucp.colegio.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.colegio.common.model.NivelEducativo;

class NombreSeccionTest {

  @Test
  void grado_usaOrdinalYNivelEnMinusculas() {
    assertThat(NombreSeccion.grado(3, NivelEducativo.SECUNDARIA)).isEqualTo("3.º de secundaria");
    assertThat(NombreSeccion.grado(5, "INICIAL")).isEqualTo("5.º de inicial");
  }

  @Test
  void grado_sinNivel_asumeSecundaria() {
    assertThat(NombreSeccion.grado(1, (String) null)).isEqualTo("1.º de secundaria");
    assertThat(NombreSeccion.grado(1, (NivelEducativo) null)).isEqualTo("1.º de secundaria");
  }

  @Test
  void completa_agregaLaLetraSinLaPalabraSeccion() {
    assertThat(NombreSeccion.completa(1, "A")).isEqualTo("1.º de secundaria A");
    assertThat(NombreSeccion.completa(2, "B", NivelEducativo.PRIMARIA))
        .isEqualTo("2.º de primaria B");
    assertThat(NombreSeccion.completa(4, "C", "SECUNDARIA")).isEqualTo("4.º de secundaria C");
  }
}
