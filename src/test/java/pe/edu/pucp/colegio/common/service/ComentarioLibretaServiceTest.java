package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.repository.ApoderadoRepository;
import pe.edu.pucp.colegio.common.repository.ComentarioLibretaRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;
import pe.edu.pucp.colegio.common.repository.VinculoFamiliarRepository;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ComentarioLibretaServiceTest {

  @Mock ComentarioLibretaRepository comentarios;
  @Mock MatriculaRepository matriculas;
  @Mock CuentaUsuarioRepository cuentas;
  @Mock SesionHorarioRepository asignaciones;
  @Mock ApoderadoRepository apoderados;
  @Mock VinculoFamiliarRepository vinculos;
  @Mock Clock clock;

  @InjectMocks ComentarioLibretaService service;

  @Test
  void obtener_sinMatriculaConfirmada_respondeNotFound() {
    when(matriculas.findByEstudianteIdAndPeriodoIdAndEstado(3, 7, EstadoMatricula.CONFIRMADA))
        .thenReturn(Optional.empty());

    ResponseStatusException error =
        assertThrows(ResponseStatusException.class, () -> service.obtener(3, 7, "alumno"));

    assertEquals(404, error.getStatusCode().value());
    verifyNoInteractions(comentarios, cuentas, asignaciones, apoderados, vinculos);
  }
}
