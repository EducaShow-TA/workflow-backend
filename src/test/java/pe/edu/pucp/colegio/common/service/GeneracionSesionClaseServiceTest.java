package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.repository.DiaNoLectivoRepository;
import pe.edu.pucp.colegio.common.repository.HorarioSeccionRepository;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;

@ExtendWith(MockitoExtension.class)
class GeneracionSesionClaseServiceTest {

  @Mock PeriodoRepository periodos;
  @Mock HorarioSeccionRepository horarios;
  @Mock SesionHorarioRepository asignaciones;
  @Mock SesionClaseRepository sesiones;
  @Mock DiaNoLectivoRepository diasNoLectivos;
  @Mock Clock clock;

  @InjectMocks GeneracionSesionClaseService service;

  @Test
  void generarPeriodo_inexistente_respondeNotFound() {
    when(periodos.findById(42)).thenReturn(Optional.empty());

    ResponseStatusException error =
        assertThrows(ResponseStatusException.class, () -> service.generarPeriodo(42));

    assertEquals(404, error.getStatusCode().value());
  }
}
