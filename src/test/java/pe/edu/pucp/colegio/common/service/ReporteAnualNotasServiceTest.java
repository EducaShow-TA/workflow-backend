package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.edu.pucp.colegio.common.dto.LibretaNotasResponse;
import pe.edu.pucp.colegio.common.dto.ReporteAnualNotasResponse;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.repository.PeriodoRepository;

class ReporteAnualNotasServiceTest {

  private final LibretaNotasService libretas = mock(LibretaNotasService.class);
  private final PeriodoRepository periodos = mock(PeriodoRepository.class);
  private final CalendarioBimestralService calendario = mock(CalendarioBimestralService.class);
  private final ReporteAnualNotasService servicio =
      new ReporteAnualNotasService(libretas, periodos, calendario);

  @Test
  void limitaElReporteAlBimestreActualYNoGeneraBimestresFuturos() {
    Periodo periodo = mock(Periodo.class);
    when(periodos.findById(12)).thenReturn(Optional.of(periodo));
    when(calendario.actual(periodo)).thenReturn(2);
    when(libretas.generar(8, 12, 1, "apoderado")).thenReturn(respuestaBimestral(1, "AD"));
    when(libretas.generar(8, 12, 2, "apoderado")).thenReturn(respuestaBimestral(2, "A"));

    ReporteAnualNotasResponse reporte = servicio.generar(8, 12, "apoderado");

    assertEquals(2, reporte.ultimoBimestre());
    assertEquals(
        List.of("AD", "A"), reporte.cursos().get(0).competencias().get(0).notasPorBimestre());
    verify(libretas).generar(8, 12, 1, "apoderado");
    verify(libretas).generar(8, 12, 2, "apoderado");
    verify(libretas, never()).generar(8, 12, 3, "apoderado");
  }

  private LibretaNotasResponse respuestaBimestral(int bimestre, String nota) {
    var competencia =
        new LibretaNotasResponse.CompetenciaLibreta(
            "MAT-1", "Resuelve problemas", nota, null, null, List.of());
    var area = new LibretaNotasResponse.AreaLibreta("MAT", "Matemática", List.of(competencia));
    var periodo =
        new LibretaNotasResponse.PeriodoLibreta(
            12, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 20), 1, "A", false, List.of(area));
    return new LibretaNotasResponse(8, "Estudiante", 12, 2026, bimestre, 0, List.of(periodo));
  }
}
