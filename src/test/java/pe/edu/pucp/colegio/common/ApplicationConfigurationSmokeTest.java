package pe.edu.pucp.colegio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class ApplicationConfigurationSmokeTest {

  @Autowired Environment environment;

  @Test
  void iniciaConLasPropiedadesPrincipalesYLosOverridesDeTestFusionados() {
    assertEquals("false", environment.getProperty("spring.jpa.open-in-view"));
    assertEquals("100", environment.getProperty("spring.data.web.pageable.max-page-size"));
    assertEquals("10MB", environment.getProperty("spring.servlet.multipart.max-file-size"));
    assertEquals("11MB", environment.getProperty("spring.servlet.multipart.max-request-size"));
    assertEquals("500.00", environment.getProperty("app.finanzas.monto-pension-predeterminado"));
    assertTrue(environment.getProperty("spring.datasource.url").startsWith("jdbc:h2:mem:colegio"));
  }
}
