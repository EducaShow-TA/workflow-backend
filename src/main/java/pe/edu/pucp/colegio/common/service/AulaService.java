package pe.edu.pucp.colegio.common.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AulaRequest;
import pe.edu.pucp.colegio.common.dto.AulaResponse;
import pe.edu.pucp.colegio.common.dto.CambioEstadoAulaRequest;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.Aula;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;
import pe.edu.pucp.colegio.common.repository.AulaRepository;
import pe.edu.pucp.colegio.common.repository.MatriculaRepository;
import pe.edu.pucp.colegio.common.repository.SesionHorarioRepository;

/** Administra las aulas físicas y su capacidad. */
@Service
public class AulaService {

  private final AulaRepository aulas;
  private final SesionHorarioRepository sesiones;
  private final MatriculaRepository matriculas;

  public AulaService(
          AulaRepository aulas, SesionHorarioRepository sesiones, MatriculaRepository matriculas) {
    this.aulas = aulas;
    this.sesiones = sesiones;
    this.matriculas = matriculas;
  }

  @Transactional(readOnly = true)
  public List<AulaResponse> listar() {
    return aulas.findAllByOrderByCodigoAsc().stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public AulaResponse obtener(Integer id) {
    return respuesta(buscar(id));
  }

  @Transactional
  public AulaResponse crear(AulaRequest request) {
    String codigo = request.codigo().trim();
    if (aulas.existsByCodigoIgnoreCase(codigo)) {
      throw new ConflictException("Ya existe un aula con el código " + codigo);
    }
    Aula aula = new Aula();
    aplicar(aula, request);
    return respuesta(aulas.save(aula));
  }

  @Transactional
  public AulaResponse actualizar(Integer id, AulaRequest request) {
    Aula aula = buscar(id);
    String codigo = request.codigo().trim();
    if (aulas.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
      throw new ConflictException("Ya existe un aula con el código " + codigo);
    }
    if (request.capacidad() < aula.getCapacidad()) {
      validarNuevaCapacidad(id, request.capacidad());
    }
    aplicar(aula, request);
    return respuesta(aulas.save(aula));
  }

  @Transactional
  public AulaResponse cambiarEstado(Integer id, CambioEstadoAulaRequest request) {
    Aula aula = buscar(id);
    if (!request.activo() && sesiones.existsByAulaId(id)) {
      throw new ConflictException(
              "No se puede desactivar el aula porque tiene sesiones de clase asignadas");
    }
    aula.setActive(request.activo());
    return respuesta(aulas.save(aula));
  }

  private void validarNuevaCapacidad(Integer aulaId, int nuevaCapacidad) {
    for (HorarioSeccion horario : sesiones.findHorariosConAula(aulaId)) {
      long alumnos =
              matriculas.countAlumnosActivosPorSeccionEnAnhio(
                      horario.getSeccion().getId(), horario.getAnio());
      if (alumnos > nuevaCapacidad) {
        throw new ConflictException(
                "No se puede reducir la capacidad a "
                        + nuevaCapacidad
                        + ": "
                        + horario.getSeccion().getGrado().getNumero()
                        + "° "
                        + horario.getSeccion().getCodigo()
                        + " tiene "
                        + alumnos
                        + " alumnos matriculados en "
                        + horario.getAnio()
                        + " y tiene clases en esta aula");
      }
    }
  }

  private void aplicar(Aula aula, AulaRequest request) {
    aula.setCodigo(request.codigo().trim());
    aula.setCapacidad(request.capacidad());
    String descripcion = request.descripcion();
    aula.setDescripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim());
  }

  private Aula buscar(Integer id) {
    return aulas
            .findById(id)
            .orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el aula indicada"));
  }

  private AulaResponse respuesta(Aula aula) {
    return new AulaResponse(
            aula.getId(),
            aula.getCodigo(),
            aula.getCapacidad(),
            aula.getDescripcion(),
            aula.getActive());
  }
}