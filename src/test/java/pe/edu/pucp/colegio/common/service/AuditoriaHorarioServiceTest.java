package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.repository.AuditoriaHorarioRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuditoriaHorarioServiceTest {

  @Mock AuditoriaHorarioRepository auditoria;
  @Mock CuentaUsuarioRepository cuentas;
  @Mock Clock clock;

  @InjectMocks AuditoriaHorarioService service;

  @Test
  void listar_conFechaInicialPosteriorAlCierre_rechazaElRango() {
    assertThrows(
        BusinessException.class,
        () -> service.listar(null, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1)));

    verifyNoInteractions(auditoria);
  }
}
