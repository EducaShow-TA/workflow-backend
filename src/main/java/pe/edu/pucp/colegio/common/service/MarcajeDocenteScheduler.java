package pe.edu.pucp.colegio.common.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ejecuta periódicamente el cierre automático de marcajes docentes vencidos. */
@Component
public class MarcajeDocenteScheduler {

  private final AsistenciaDocenteService asistencias;

  public MarcajeDocenteScheduler(AsistenciaDocenteService asistencias) {
    this.asistencias = asistencias;
  }

  @Scheduled(fixedDelayString = "${app.asistencia.procesamiento-inasistencias-ms:60000}")
  public void procesarInasistenciasVencidas() {
    asistencias.procesarInasistenciasPendientes();
  }
}
