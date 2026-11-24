package pe.edu.pucp.colegio.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse.BoletaHistorial;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse.MatriculaHistorial;
import pe.edu.pucp.colegio.common.dto.HistorialBoletasResponse.Totales;
import pe.edu.pucp.colegio.common.model.BoletaPension;
import pe.edu.pucp.colegio.common.model.ComprobantePago;
import pe.edu.pucp.colegio.common.model.CompromisoPago;
import pe.edu.pucp.colegio.common.model.EstadoBoleta;
import pe.edu.pucp.colegio.common.model.EstadoMatricula;
import pe.edu.pucp.colegio.common.model.Estudiante;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.Matricula;
import pe.edu.pucp.colegio.common.model.MecanismoGeneracion;
import pe.edu.pucp.colegio.common.model.Periodo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.BoletaPensionRepository;
import pe.edu.pucp.colegio.common.repository.ComprobantePagoRepository;
import pe.edu.pucp.colegio.common.repository.CompromisoPagoRepository;
import pe.edu.pucp.colegio.common.repository.EstudianteRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;

@ExtendWith(MockitoExtension.class)
class HistorialBoletasServiceTest {

  private static final int ESTUDIANTE = 4;

  @Mock EstudianteRepository estudiantes;
  @Mock MatriculaRepository matriculas;
  @Mock CompromisoPagoRepository compromisos;
  @Mock BoletaPensionRepository boletas;
  @Mock ComprobantePagoRepository comprobantes;
  @Mock FinanzasService finanzas;

  @InjectMocks HistorialBoletasService service;

  private final List<Matricula> matriculasDelAlumno = new ArrayList<>();
  private final List<CompromisoPago> compromisosDelAlumno = new ArrayList<>();
  private final List<BoletaPension> boletasDelAlumno = new ArrayList<>();
  private final List<ComprobantePago> comprobantesDelAlumno = new ArrayList<>();

  @BeforeEach
  void preparar() {
    Persona persona = new Persona("12345678", "Ana", "Pérez", "Díaz");
    Estudiante estudiante = new Estudiante();
    ReflectionTestUtils.setField(estudiante, "id", ESTUDIANTE);
    ReflectionTestUtils.setField(estudiante, "persona", persona);
    lenient().when(estudiantes.findById(ESTUDIANTE)).thenReturn(Optional.of(estudiante));
    lenient()
        .when(finanzas.completarBoletasDeEstudiante(ESTUDIANTE))
        .thenReturn(new FinanzasService.BoletasCompletadas(YearMonth.of(2035, 6), 2, 1));
    lenient()
        .when(matriculas.findAllByEstudianteIdIn(List.of(ESTUDIANTE)))
        .thenReturn(matriculasDelAlumno);
    lenient()
        .when(compromisos.findAllByMatriculaEstudianteId(ESTUDIANTE))
        .thenReturn(compromisosDelAlumno);
    lenient()
        .when(
            boletas.findAllByCompromisoMatriculaEstudianteIdAndActiveTrueOrderByMesCobroAsc(
                ESTUDIANTE))
        .thenReturn(boletasDelAlumno);
    lenient()
        .when(comprobantes.findAllByBoletaIdInAndActiveTrue(anyList()))
        .thenReturn(comprobantesDelAlumno);
  }

  @Test
  void generar_estudianteInexistente_lanzaNotFound() {
    when(estudiantes.findById(9)).thenReturn(Optional.empty());

    ResponseStatusException error =
        assertThrows(ResponseStatusException.class, () -> service.generar(9));

    assertEquals(404, error.getStatusCode().value());
    assertEquals("No existe el estudiante indicado", error.getReason());
    verifyNoInteractions(finanzas);
  }

  @Test
  void generar_sinMatriculas_devuelveListaVacia() {
    HistorialBoletasResponse historial = service.generar(ESTUDIANTE);

    assertEquals("Ana Pérez Díaz", historial.estudiante());
    assertEquals(YearMonth.of(2035, 6), historial.mesCorte());
    assertEquals(2, historial.boletasGeneradas());
    assertEquals(1, historial.boletasEnMora());
    assertTrue(historial.matriculas().isEmpty());
    assertEquals(totales("0.00", "0.00", "0.00"), historial.totales());
    verifyNoInteractions(comprobantes);
  }

  @Test
  void generar_matriculaSinCompromiso_devuelveCompromisoNulo() {
    matricula(1, 2035, EstadoMatricula.CONFIRMADA);

    MatriculaHistorial fila = service.generar(ESTUDIANTE).matriculas().get(0);

    assertEquals(1, fila.grado());
    assertEquals("A", fila.seccion());
    assertNull(fila.compromiso());
    assertTrue(fila.boletas().isEmpty());
  }

  @Test
  void generar_boletasDeTodosLosEstados_lasIncluyeYTotaliza() {
    CompromisoPago compromiso = compromiso(5, matricula(1, 2035, EstadoMatricula.CONFIRMADA), true);
    pagar(boleta(17, compromiso, 3, EstadoBoleta.PAGADA), "PEN-1");
    boleta(18, compromiso, 4, EstadoBoleta.EN_MORA);
    boleta(19, compromiso, 5, EstadoBoleta.PENDIENTE);

    HistorialBoletasResponse historial = service.generar(ESTUDIANTE);

    assertEquals(totales("350.00", "350.00", "350.00"), historial.totales());
    MatriculaHistorial fila = historial.matriculas().get(0);
    assertEquals("Pensión 2035", fila.compromiso().concepto());
    assertEquals(
        List.of(EstadoBoleta.PAGADA, EstadoBoleta.EN_MORA, EstadoBoleta.PENDIENTE),
        fila.boletas().stream().map(BoletaHistorial::estadoPago).toList());
    assertEquals("PEN-1", fila.boletas().get(0).numeroComprobante());
    assertNull(fila.boletas().get(1).numeroComprobante());
  }

  @Test
  void generar_matriculaRetirada_muestraBoletasSinSumarDeuda() {
    CompromisoPago compromiso = compromiso(5, matricula(1, 2035, EstadoMatricula.RETIRADA), false);
    pagar(boleta(17, compromiso, 3, EstadoBoleta.PAGADA), "PEN-1");
    boleta(18, compromiso, 4, EstadoBoleta.EN_MORA);
    boleta(19, compromiso, 5, EstadoBoleta.PENDIENTE);

    HistorialBoletasResponse historial = service.generar(ESTUDIANTE);

    assertEquals(5, historial.matriculas().get(0).compromiso().id());
    assertEquals(3, historial.matriculas().get(0).boletas().size());
    assertEquals(totales("350.00", "0.00", "0.00"), historial.totales());
  }

  @Test
  void generar_variasMatriculas_ordenaDeMasRecienteAMasAntigua() {
    pagar(
        boleta(
            17,
            compromiso(5, matricula(1, 2034, EstadoMatricula.CONFIRMADA), true),
            3,
            EstadoBoleta.PAGADA),
        "PEN-1");
    pagar(
        boleta(
            18,
            compromiso(6, matricula(2, 2035, EstadoMatricula.CONFIRMADA), true),
            3,
            EstadoBoleta.PAGADA),
        "PEN-2");

    HistorialBoletasResponse historial = service.generar(ESTUDIANTE);

    assertEquals(
        List.of(2035, 2034),
        historial.matriculas().stream().map(MatriculaHistorial::anio).toList());
    assertEquals(18, historial.matriculas().get(0).boletas().get(0).id());
    assertEquals(totales("700.00", "0.00", "0.00"), historial.totales());
  }

  @Test
  void generar_matriculaInactivaNoRetirada_laOmite() {
    matricula(1, 2035, EstadoMatricula.EN_REGISTRO).setActive(false);

    assertTrue(service.generar(ESTUDIANTE).matriculas().isEmpty());
  }

  private Totales totales(String pagado, String pendiente, String enMora) {
    return new Totales(new BigDecimal(pagado), new BigDecimal(pendiente), new BigDecimal(enMora));
  }

  private Matricula matricula(int id, int anio, EstadoMatricula estado) {
    Grado grado = new Grado();
    ReflectionTestUtils.setField(grado, "numero", 1);
    Seccion seccion = new Seccion();
    ReflectionTestUtils.setField(seccion, "codigo", "A");
    Periodo periodo = new Periodo();
    ReflectionTestUtils.setField(periodo, "grado", grado);
    ReflectionTestUtils.setField(periodo, "seccion", seccion);
    ReflectionTestUtils.setField(periodo, "fechaInicio", LocalDate.of(anio, 3, 1));
    Matricula matricula = new Matricula();
    ReflectionTestUtils.setField(matricula, "id", id);
    ReflectionTestUtils.setField(matricula, "periodo", periodo);
    matricula.setEstado(estado);
    matricula.setFechaMatricula(LocalDate.of(anio, 2, 1));
    matriculasDelAlumno.add(matricula);
    return matricula;
  }

  private CompromisoPago compromiso(int id, Matricula matricula, boolean activo) {
    CompromisoPago compromiso = new CompromisoPago();
    ReflectionTestUtils.setField(compromiso, "id", id);
    compromiso.setMatricula(matricula);
    compromiso.setConcepto("Pensión " + matricula.getPeriodo().getFechaInicio().getYear());
    compromiso.setNumeroCuotas(6);
    compromiso.setMesInicio(YearMonth.of(2035, 3));
    compromiso.setActive(activo);
    compromisosDelAlumno.add(compromiso);
    return compromiso;
  }

  private BoletaPension boleta(int id, CompromisoPago compromiso, int mes, EstadoBoleta estado) {
    BoletaPension boleta = new BoletaPension();
    ReflectionTestUtils.setField(boleta, "id", id);
    boleta.setCompromiso(compromiso);
    boleta.setMesCobro(YearMonth.of(2035, mes));
    boleta.setFechaVencimiento(LocalDate.of(2035, mes, 15));
    boleta.setMonto(new BigDecimal("350.00"));
    boleta.setMora(new BigDecimal("0.00"));
    boleta.setEstadoPago(estado);
    boleta.setMecanismoGeneracion(MecanismoGeneracion.AUTOMATICO);
    boletasDelAlumno.add(boleta);
    return boleta;
  }

  private void pagar(BoletaPension boleta, String numero) {
    ComprobantePago comprobante = new ComprobantePago();
    comprobante.setBoleta(boleta);
    comprobante.setNumeroComprobante(numero);
    comprobantesDelAlumno.add(comprobante);
  }
}
