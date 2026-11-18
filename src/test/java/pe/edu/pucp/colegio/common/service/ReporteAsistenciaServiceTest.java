package pe.edu.pucp.colegio.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.FilaReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.ReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.dto.SeccionReporteAsistenciaResponse;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Persona;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.AsistenciaEstudianteRepository;
import pe.edu.pucp.colegio.common.repository.ConsolidadoAsistenciaFila;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;
import pe.edu.pucp.colegio.common.repository.SesionClaseRepository;
import pe.edu.pucp.colegio.security.model.CuentaUsuario;
import pe.edu.pucp.colegio.security.model.Rol;
import pe.edu.pucp.colegio.security.repository.CuentaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReporteAsistenciaServiceTest {

  private static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
  private static final LocalDate HASTA = LocalDate.of(2026, 11, 17);
  private static final int PERSONA_DOCENTE = 30;

  @Mock AsistenciaEstudianteRepository asistencias;
  @Mock SesionClaseRepository sesiones;
  @Mock SeccionRepository secciones;
  @Mock CuentaUsuarioRepository cuentas;
  @InjectMocks ReporteAsistenciaService service;

  @Test
  void generar_rangoInvertido_lanzaBusinessException() {
    assertThatThrownBy(() -> service.generar(HASTA, DESDE, null, null, "admin"))
        .isInstanceOf(BusinessException.class);
    verify(asistencias, never()).consolidarPorAlumno(any(), any(), any(), any(), any());
  }

  @Test
  void generar_administrador_noFiltraPorDocente() {
    cuenta("admin", Rol.ADMINISTRADOR);

    service.generar(DESDE, HASTA, 3, 7, "admin");

    verify(asistencias).consolidarPorAlumno(DESDE, HASTA, 3, 7, null);
  }

  @Test
  void generar_docenteConSeccionAjena_lanza403() {
    cuenta("docente", Rol.DOCENTE);

    assertThatThrownBy(() -> service.generar(DESDE, HASTA, null, 7, "docente"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            e -> {
              assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
              assertThat(e.getReason()).contains("rango de fechas seleccionado");
            });
    verify(asistencias, never()).consolidarPorAlumno(any(), any(), any(), any(), any());
  }

  @Test
  void generar_docenteConSesionDictadaSinHorario_permite() {
    cuenta("docente", Rol.DOCENTE);
    when(sesiones.existsByDocentePersonaIdAndSeccionIdAndFechaBetweenAndActiveTrue(
            PERSONA_DOCENTE, 7, DESDE, HASTA))
        .thenReturn(true);

    service.generar(DESDE, HASTA, null, 7, "docente");

    verify(asistencias).consolidarPorAlumno(DESDE, HASTA, null, 7, PERSONA_DOCENTE);
  }

  @Test
  void generar_docenteConHorarioEnSeccion_permite() {
    cuenta("docente", Rol.DOCENTE);
    when(sesiones.docenteTieneClaseEnSeccion(PERSONA_DOCENTE, 7, DESDE, HASTA)).thenReturn(true);

    service.generar(DESDE, HASTA, null, 7, "docente");

    verify(asistencias).consolidarPorAlumno(DESDE, HASTA, null, 7, PERSONA_DOCENTE);
  }

  @Test
  void generar_docenteSinSeccion_filtraPorSuPersona() {
    cuenta("docente", Rol.DOCENTE);

    service.generar(DESDE, HASTA, null, null, "docente");

    verify(asistencias).consolidarPorAlumno(DESDE, HASTA, null, null, PERSONA_DOCENTE);
  }

  @Test
  void generar_docenteConGradoAjenoSinSeccion_devuelveVacio() {
    cuenta("docente", Rol.DOCENTE);
    when(asistencias.consolidarPorAlumno(DESDE, HASTA, 9, null, PERSONA_DOCENTE))
        .thenReturn(List.of());

    ReporteAsistenciaResponse reporte = service.generar(DESDE, HASTA, 9, null, "docente");

    assertThat(reporte.alumnos()).isEmpty();
    assertThat(reporte.totales().totalSesiones()).isZero();
    assertThat(reporte.desde()).isEqualTo(DESDE);
    assertThat(reporte.hasta()).isEqualTo(HASTA);
  }

  @Test
  void generar_alumnoOApoderado_lanza403() {
    cuenta("alumno", Rol.ALUMNO);
    cuenta("apoderado", Rol.APODERADO);

    for (String usuario : List.of("alumno", "apoderado")) {
      assertThatThrownBy(() -> service.generar(DESDE, HASTA, null, null, usuario))
          .isInstanceOfSatisfying(
              ResponseStatusException.class,
              e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
  }

  @Test
  void porcentaje_sinSesiones_devuelveCeroConUnDecimal() {
    assertThat(ReporteAsistenciaService.porcentaje(0, 0)).isEqualByComparingTo("0.0");
    assertThat(ReporteAsistenciaService.porcentaje(0, 0).scale()).isEqualTo(1);
  }

  @Test
  void generar_variasFilas_calculaPorcentajeYTotales() {
    cuenta("admin", Rol.ADMINISTRADOR);
    when(asistencias.consolidarPorAlumno(DESDE, HASTA, null, null, null))
        .thenReturn(
            List.of(
                new Fila(1, "Ana", "Quispe", "Rojas", 40, 5, 6, 1),
                new Fila(2, "Beto", "Diaz", "Soto", 10, 0, 0, 0)));

    ReporteAsistenciaResponse reporte = service.generar(DESDE, HASTA, null, null, "admin");

    FilaReporteAsistenciaResponse ana = reporte.alumnos().get(0);
    assertThat(ana.totalSesiones()).isEqualTo(52);
    assertThat(ana.porcentajeInasistencia()).isEqualByComparingTo(new BigDecimal("11.5"));
    assertThat(reporte.totales().presentes()).isEqualTo(50);
    assertThat(reporte.totales().tardanzas()).isEqualTo(5);
    assertThat(reporte.totales().inasistencias()).isEqualTo(6);
    assertThat(reporte.totales().justificadas()).isEqualTo(1);
    assertThat(reporte.totales().totalSesiones()).isEqualTo(62);
  }

  @Test
  void generar_variosAlumnos_ordenaPorPorcentajeDescYNombre() {
    cuenta("admin", Rol.ADMINISTRADOR);
    when(asistencias.consolidarPorAlumno(DESDE, HASTA, null, null, null))
        .thenReturn(
            List.of(
                new Fila(1, "Zoe", "Alva", "Mena", 9, 0, 1, 0), // 10.0 %
                new Fila(2, "Carla", "Mena", "Alva", 18, 0, 2, 0), // 10.0 %, más inasistencias
                new Fila(3, "Bruno", "Alva", "Mena", 9, 0, 1, 0), // 10.0 %, empate con Zoe
                new Fila(4, "Ana", "Paz", "Ruiz", 1, 0, 1, 0))); // 50.0 %

    List<Integer> orden =
        service.generar(DESDE, HASTA, null, null, "admin").alumnos().stream()
            .map(FilaReporteAsistenciaResponse::estudianteId)
            .toList();

    assertThat(orden).containsExactly(4, 2, 3, 1);
  }

  @Test
  void generar_fila_armaEtiquetasDeGradoYNombre() {
    cuenta("admin", Rol.ADMINISTRADOR);
    when(asistencias.consolidarPorAlumno(DESDE, HASTA, null, null, null))
        .thenReturn(List.of(new Fila(1, "Ana", "Quispe", "Rojas", 1, 0, 0, 0)));

    FilaReporteAsistenciaResponse fila =
        service.generar(DESDE, HASTA, null, null, "admin").alumnos().get(0);

    assertThat(fila.nombreCompleto()).isEqualTo("Ana Quispe Rojas");
    assertThat(fila.grado()).isEqualTo("3.° Secundaria");
    assertThat(fila.gradoId()).isEqualTo(3);
    assertThat(fila.seccionId()).isEqualTo(7);
    assertThat(fila.seccion()).isEqualTo("A");
  }

  @Test
  void listarSecciones_administrador_ordenaPorNivelNumeroYCodigo() {
    cuenta("admin", Rol.ADMINISTRADOR);
    List<Seccion> activas =
        List.of(
            seccion(10, "A", 1, 1, NivelEducativo.SECUNDARIA),
            seccion(11, "B", 2, 1, NivelEducativo.PRIMARIA),
            seccion(12, "A", 2, 1, NivelEducativo.PRIMARIA),
            seccion(13, "A", 3, 5, NivelEducativo.INICIAL));
    when(secciones.findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc()).thenReturn(activas);

    List<SeccionReporteAsistenciaResponse> resultado = service.listarSecciones("admin");

    assertThat(resultado)
        .extracting(SeccionReporteAsistenciaResponse::seccionId)
        .containsExactly(13, 12, 11, 10);
    assertThat(resultado.get(0).grado()).isEqualTo("5.° Inicial");
    assertThat(resultado.get(0).gradoId()).isEqualTo(3);
  }

  @Test
  void listarSecciones_docente_usaBuscarSeccionesDelDocente() {
    cuenta("docente", Rol.DOCENTE);
    List<Seccion> propias = List.of(seccion(10, "A", 1, 1, NivelEducativo.SECUNDARIA));
    when(secciones.buscarSeccionesDelDocente(PERSONA_DOCENTE)).thenReturn(propias);

    assertThat(service.listarSecciones("docente"))
        .extracting(SeccionReporteAsistenciaResponse::seccionId)
        .containsExactly(10);
    verify(secciones, never()).findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc();
  }

  @Test
  void listarSecciones_alumno_lanza403() {
    cuenta("alumno", Rol.ALUMNO);

    assertThatThrownBy(() -> service.listarSecciones("alumno"))
        .isInstanceOf(ResponseStatusException.class);
    verify(secciones, never()).buscarSeccionesDelDocente(any());
    verify(secciones, never()).findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc();
  }

  private void cuenta(String usuario, Rol rol) {
    CuentaUsuario cuenta = mock(CuentaUsuario.class);
    Persona persona = mock(Persona.class);
    lenient().when(persona.getId()).thenReturn(PERSONA_DOCENTE);
    lenient().when(cuenta.getActive()).thenReturn(true);
    lenient().when(cuenta.getRol()).thenReturn(rol);
    lenient().when(cuenta.getPersona()).thenReturn(persona);
    lenient().when(cuentas.findByUsername(usuario)).thenReturn(Optional.of(cuenta));
  }

  private Seccion seccion(int id, String codigo, int gradoId, int numero, NivelEducativo nivel) {
    Grado grado = mock(Grado.class);
    lenient().when(grado.getId()).thenReturn(gradoId);
    lenient().when(grado.getNumero()).thenReturn(numero);
    lenient().when(grado.getNivelEducativo()).thenReturn(nivel);
    Seccion seccion = mock(Seccion.class);
    lenient().when(seccion.getId()).thenReturn(id);
    lenient().when(seccion.getCodigo()).thenReturn(codigo);
    lenient().when(seccion.getGrado()).thenReturn(grado);
    return seccion;
  }

  private record Fila(
      Integer estudianteId,
      String nombre,
      String apellidoPaterno,
      String apellidoMaterno,
      long presentes,
      long tardanzas,
      long inasistencias,
      long justificadas)
      implements ConsolidadoAsistenciaFila {

    @Override
    public Integer getEstudianteId() {
      return estudianteId;
    }

    @Override
    public String getNombre() {
      return nombre;
    }

    @Override
    public String getApellidoPaterno() {
      return apellidoPaterno;
    }

    @Override
    public String getApellidoMaterno() {
      return apellidoMaterno;
    }

    @Override
    public Integer getGradoId() {
      return 3;
    }

    @Override
    public Integer getGradoNumero() {
      return 3;
    }

    @Override
    public NivelEducativo getNivelEducativo() {
      return NivelEducativo.SECUNDARIA;
    }

    @Override
    public Integer getSeccionId() {
      return 7;
    }

    @Override
    public String getSeccionCodigo() {
      return "A";
    }

    @Override
    public Long getPresentes() {
      return presentes;
    }

    @Override
    public Long getTardanzas() {
      return tardanzas;
    }

    @Override
    public Long getInasistencias() {
      return inasistencias;
    }

    @Override
    public Long getJustificadas() {
      return justificadas;
    }
  }
}
