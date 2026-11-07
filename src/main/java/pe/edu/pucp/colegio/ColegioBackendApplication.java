package pe.edu.pucp.colegio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ColegioBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(ColegioBackendApplication.class, args);
  }
}
