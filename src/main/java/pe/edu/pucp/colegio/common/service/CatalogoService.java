package pe.edu.pucp.colegio.common.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.*;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Competencia;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.CompetenciaRepository;

@Service
@Transactional
public class CatalogoService {

  private final AreaRepository areas;
  private final CompetenciaRepository competencias;

  public CatalogoService(AreaRepository areas, CompetenciaRepository competencias) {
    this.areas = areas;
    this.competencias = competencias;
  }

  @Transactional(readOnly = true)
  public AreaResponse obtenerArea(String codigo) {
    return toResponse(area(codigo));
  }

  public AreaResponse crearArea(AreaRequest request) {
    if (areas.existsById(request.codigo())) {
      throw conflict("Ya existe un área con ese código");
    }
    Area area = new Area();
    area.setCodigo(request.codigo());
    aplicar(area, request);
    return toResponse(areas.save(area));
  }

  public AreaResponse actualizarArea(String codigo, AreaRequest request) {
    validarCodigo(codigo, request.codigo());
    Area area = area(codigo);
    aplicar(area, request);
    return toResponse(areas.save(area));
  }

  public void eliminarArea(String codigo) {
    Area area = area(codigo);
    desvincular(area);
    areas.delete(area);
  }

  @Transactional(readOnly = true)
  public CompetenciaResponse obtenerCompetencia(String codigo) {
    return toResponse(competencia(codigo));
  }

  public CompetenciaResponse crearCompetencia(CompetenciaRequest request) {
    if (competencias.existsById(request.codigo())) {
      throw conflict("Ya existe una competencia con ese código");
    }
    Competencia competencia = new Competencia();
    competencia.setCodigo(request.codigo());
    competencia.setNombre(request.nombre());
    competencia.setDescripcion(request.descripcion());
    competencia = competencias.save(competencia);
    aplicar(competencia, request);
    return toResponse(competencias.save(competencia));
  }

  public CompetenciaResponse actualizarCompetencia(String codigo, CompetenciaRequest request) {
    validarCodigo(codigo, request.codigo());
    Competencia competencia = competencia(codigo);
    aplicar(competencia, request);
    return toResponse(competencias.save(competencia));
  }

  public void eliminarCompetencia(String codigo) {
    Competencia competencia = competencia(codigo);
    for (Area area : new LinkedHashSet<>(competencia.getAreas())) {
      area.getCompetencias().remove(competencia);
    }
    competencias.delete(competencia);
  }

  private void aplicar(Area area, AreaRequest request) {
    area.setNombre(request.nombre());
    area.setDescripcion(request.descripcion());
    desvincular(area);
    for (Competencia competencia : buscarCompetencias(request.competenciaCodigos())) {
      area.getCompetencias().add(competencia);
      competencia.getAreas().add(area);
    }
  }

  private void aplicar(Competencia competencia, CompetenciaRequest request) {
    competencia.setNombre(request.nombre());
    competencia.setDescripcion(request.descripcion());
    for (Area area : new LinkedHashSet<>(competencia.getAreas())) {
      area.getCompetencias().remove(competencia);
    }
    competencia.getAreas().clear();
    for (Area area : buscarAreas(request.areaCodigos())) {
      area.getCompetencias().add(competencia);
      competencia.getAreas().add(area);
    }
  }

  private void desvincular(Area area) {
    for (Competencia competencia : new LinkedHashSet<>(area.getCompetencias())) {
      competencia.getAreas().remove(area);
    }
    area.getCompetencias().clear();
  }

  private Set<Competencia> buscarCompetencias(Set<String> codigos) {
    if (codigos == null || codigos.isEmpty()) return new LinkedHashSet<>();
    List<Competencia> encontradas = competencias.findAllById(codigos);
    if (encontradas.size() != codigos.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más códigos de competencia no existen");
    }
    return new LinkedHashSet<>(encontradas);
  }

  private Set<Area> buscarAreas(Set<String> codigos) {
    if (codigos == null || codigos.isEmpty()) return new LinkedHashSet<>();
    List<Area> encontradas = areas.findAllById(codigos);
    if (encontradas.size() != codigos.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más códigos de área no existen");
    }
    return new LinkedHashSet<>(encontradas);
  }

  private Area area(String codigo) {
    return areas
        .findById(codigo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Área no encontrada"));
  }

  private Competencia competencia(String codigo) {
    return competencias
        .findById(codigo)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competencia no encontrada"));
  }

  private void validarCodigo(String pathCodigo, String bodyCodigo) {
    if (!pathCodigo.equals(bodyCodigo)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El código del cuerpo debe coincidir con el de la URL");
    }
  }

  private AreaResponse toResponse(Area area) {
    return new AreaResponse(
        area.getCodigo(),
        area.getNombre(),
        area.getDescripcion(),
        area.getCompetencias().stream()
            .map(Competencia::getCodigo)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
  }

  private CompetenciaResponse toResponse(Competencia competencia) {
    return new CompetenciaResponse(
        competencia.getCodigo(),
        competencia.getNombre(),
        competencia.getDescripcion(),
        competencia.getAreas().stream()
            .map(Area::getCodigo)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
  }

  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
