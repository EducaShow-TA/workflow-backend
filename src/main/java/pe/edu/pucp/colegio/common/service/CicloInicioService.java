package pe.edu.pucp.colegio.common.service;

import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

/**
 * Recupera al iniciar los ciclos diarios que no se ejecutaron mientras la aplicación estuvo
 * apagada.
 */
@Component
@ConditionalOnProperty(
    prefix = "app.jobs",
    name = "catch-up-on-startup",
    havingValue = "true",
    matchIfMissing = true)
public class CicloInicioService {

  private static final Logger log = LoggerFactory.getLogger(CicloInicioService.class);

  private final FinanzasService finanzas;
  private final GeneracionSesionClaseService generacionSesiones;
  private final PeriodoRepository periodos;
  private final Clock clock;

  public CicloInicioService(
      FinanzasService finanzas,
      GeneracionSesionClaseService generacionSesiones,
      PeriodoRepository periodos,
      Clock clock) {
    this.finanzas = finanzas;
    this.generacionSesiones = generacionSesiones;
    this.periodos = periodos;
    this.clock = clock;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void recuperarCiclosAlIniciar() {
    procesarFinanzas();
    generarSesionesPendientes();
  }

  private void procesarFinanzas() {
    try {
      finanzas.procesarCicloDiario();
      log.info("event=catchup_finanzas_completado");
    } catch (RuntimeException e) {
      log.error("event=catchup_finanzas_fallido", e);
    }
  }

  private void generarSesionesPendientes() {
    LocalDate hoy = LocalDate.now(clock);
    for (Periodo periodo : periodos.findAllByActiveTrueAndFechaFinGreaterThanEqual(hoy)) {
      try {
        var resultado = generacionSesiones.generarPeriodo(periodo.getId());
        log.info(
            "event=catchup_sesiones_completado periodoId={} creadas={} canceladas={} omitidasNoLectivas={} omitidasDocenteInactivo={}",
            resultado.periodoId(),
            resultado.creadas(),
            resultado.canceladasPorCalendario(),
            resultado.omitidasNoLectivas(),
            resultado.omitidasDocenteInactivo());
      } catch (RuntimeException e) {
        log.warn(
            "event=catchup_sesiones_fallido periodoId={} causa={}",
            periodo.getId(),
            e.getMessage());
      }
    }
  }
}
