package pe.edu.pucp.colegio.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AsistenciaProperties.class)
public class AsistenciaConfiguration {

  @Bean
  Clock applicationClock() {
    return Clock.system(ZoneId.of("America/Lima"));
  }
}
