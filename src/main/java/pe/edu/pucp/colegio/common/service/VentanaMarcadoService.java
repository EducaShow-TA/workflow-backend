package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.config.AsistenciaProperties;

/** Valida que el marcaje del alumno ocurra dentro de la ventana configurada de su sesión. */
@Service
public class VentanaMarcadoService {

  private final AsistenciaProperties properties;
  private final Clock clock;

  public VentanaMarcadoService(AsistenciaProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public LocalDateTime apertura(LocalDate fechaSesion, LocalTime horaInicio) {
    return LocalDateTime.of(fechaSesion, horaInicio)
        .minusMinutes(properties.getVentanaMarcadoAntesMinutos());
  }

  public LocalDateTime cierre(LocalDate fechaSesion, LocalTime horaInicio) {
    return LocalDateTime.of(fechaSesion, horaInicio)
        .plusMinutes(properties.getVentanaMarcadoDespuesMinutos());
  }

  public boolean estaAbierta(LocalDate fechaSesion, LocalTime horaInicio) {
    LocalDateTime ahora = LocalDateTime.now(clock);
    return !ahora.isBefore(apertura(fechaSesion, horaInicio))
        && !ahora.isAfter(cierre(fechaSesion, horaInicio));
  }

  public void validar(LocalDate fechaSesion, LocalTime horaInicio) {
    if (!estaAbierta(fechaSesion, horaInicio)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El marcaje está fuera de la ventana permitida para esta sesión");
    }
  }
}
