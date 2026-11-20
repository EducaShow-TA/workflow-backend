package pe.edu.pucp.colegio.common.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarcajeDocenteSchedulerTest {

  @Mock AsistenciaDocenteService asistencias;

  @InjectMocks MarcajeDocenteScheduler scheduler;

  @Test
  void procesaLasInasistenciasDocentesVencidas() {
    scheduler.procesarInasistenciasVencidas();

    verify(asistencias).procesarInasistenciasPendientes();
  }
}
