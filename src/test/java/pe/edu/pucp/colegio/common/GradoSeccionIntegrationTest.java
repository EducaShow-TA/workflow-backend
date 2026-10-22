package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Seccion;

@SpringBootTest
@Transactional
class GradoSeccionIntegrationTest {

  @PersistenceContext EntityManager entityManager;

  @Test
  void gradoPuedePersistirUnaOMasSeccionesYSeccionPerteneceAlGrado() {
    Grado grado = new Grado();
    grado.setNumero(ThreadLocalRandom.current().nextInt(1, 100_000));
    grado.setNivelEducativo(NivelEducativo.SECUNDARIA);

    Seccion seccionA = nuevaSeccion("A");
    Seccion seccionB = nuevaSeccion("B");
    grado.agregarSeccion(seccionA);
    grado.agregarSeccion(seccionB);

    entityManager.persist(grado);
    entityManager.flush();
    Integer gradoId = grado.getId();
    Integer seccionId = seccionA.getId();
    entityManager.clear();

    Grado guardado = entityManager.find(Grado.class, gradoId);
    Seccion seccionGuardada = entityManager.find(Seccion.class, seccionId);

    assertEquals(2, guardado.getSecciones().size());
    assertNotNull(seccionGuardada.getGrado());
    assertEquals(gradoId, seccionGuardada.getGrado().getId());
  }

  @Test
  void numeroDeGradoSePuedeRepetirEnNivelesEducativosDistintos() {
    Grado primaria = nuevoGrado(99, NivelEducativo.PRIMARIA);
    primaria.agregarSeccion(nuevaSeccion("A"));
    Grado secundaria = nuevoGrado(99, NivelEducativo.SECUNDARIA);
    secundaria.agregarSeccion(nuevaSeccion("A"));

    entityManager.persist(primaria);
    entityManager.persist(secundaria);
    entityManager.flush();
  }

  private Grado nuevoGrado(Integer numero, NivelEducativo nivel) {
    Grado grado = new Grado();
    grado.setNumero(numero);
    grado.setNivelEducativo(nivel);
    return grado;
  }

  private Seccion nuevaSeccion(String codigo) {
    Seccion seccion = new Seccion();
    seccion.setCodigo(codigo);
    seccion.setVacantesTotales("30");
    return seccion;
  }
}
