package pe.edu.pucp.colegio.common.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.SeccionCreateRequest;
import pe.edu.pucp.colegio.common.dto.SeccionResponse;
import pe.edu.pucp.colegio.common.dto.SeccionVacantesRequest;
import pe.edu.pucp.colegio.common.model.Grado;
import pe.edu.pucp.colegio.common.model.NivelEducativo;
import pe.edu.pucp.colegio.common.model.Seccion;
import pe.edu.pucp.colegio.common.repository.GradoRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SeccionRepository;

/** Gestiona secciones y valida su capacidad frente a las matrículas activas. */
@Service
public class SeccionService {

  private final SeccionRepository secciones;
  private final GradoRepository grados;
  private final MatriculaRepository matriculas;

  public SeccionService(
      SeccionRepository secciones, GradoRepository grados, MatriculaRepository matriculas) {
    this.secciones = secciones;
    this.grados = grados;
    this.matriculas = matriculas;
  }

  @Transactional(readOnly = true)
  public List<SeccionResponse> listar() {
    return secciones.findAllByActiveTrueOrderByGradoNumeroAscCodigoAsc().stream()
        .map(this::respuesta)
        .toList();
  }

  @Transactional
  public SeccionResponse crear(SeccionCreateRequest request) {
    Grado grado =
        grados
            .findByNumeroAndNivelEducativoAndActiveTrue(
                request.gradoNumero(), NivelEducativo.SECUNDARIA)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el grado de secundaria indicado"));
    if (secciones.existsByGradoIdAndCodigoIgnoreCase(grado.getId(), request.nombre())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La sección ya existe para el grado indicado");
    }

    Seccion seccion = new Seccion();
    seccion.setCodigo(request.nombre());
    seccion.setVacantesTotales(request.vacantesTotales());
    grado.agregarSeccion(seccion);
    seccion = secciones.save(seccion);
    return respuesta(seccion);
  }

  @Transactional
  public SeccionResponse actualizarVacantes(Integer id, SeccionVacantesRequest request) {
    Seccion seccion = bloquearSeccion(id);
    long matriculados = matriculas.countAlumnosActivosPorSeccion(id);
    if (request.vacantesTotales() < matriculados) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "La capacidad no puede ser menor que los " + matriculados + " alumnos ya matriculados");
    }
    seccion.setVacantesTotales(request.vacantesTotales());
    return respuesta(seccion);
  }

  private Seccion bloquearSeccion(Integer id) {
    return secciones
        .findByIdForUpdate(id)
        .filter(seccion -> Boolean.TRUE.equals(seccion.getActive()))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sección no existe"));
  }

  private SeccionResponse respuesta(Seccion seccion) {
    long matriculados = matriculas.countAlumnosActivosPorSeccion(seccion.getId());
    Integer capacidad = seccion.getVacantesTotales();
    int capacidadVisible = capacidad == null || capacidad <= 0 ? 0 : capacidad;
    return new SeccionResponse(
        seccion.getId(),
        seccion.getGrado().getNumero(),
        seccion.getCodigo(),
        capacidadVisible,
        matriculados,
        Math.max(0, capacidadVisible - matriculados));
  }
}
