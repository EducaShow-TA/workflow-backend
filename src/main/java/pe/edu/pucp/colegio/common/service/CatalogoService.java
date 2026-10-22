package pe.edu.pucp.colegio.common.service;

import jakarta.persistence.EntityManager;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.AreaCatalogoCreateRequest;
import pe.edu.pucp.colegio.common.dto.AreaRequest;
import pe.edu.pucp.colegio.common.dto.AreaResponse;
import pe.edu.pucp.colegio.common.dto.CambioEstadoCatalogoRequest;
import pe.edu.pucp.colegio.common.dto.CatalogoAreaResponse;
import pe.edu.pucp.colegio.common.dto.CatalogoCompetenciaResponse;
import pe.edu.pucp.colegio.common.dto.CompetenciaNuevaRequest;
import pe.edu.pucp.colegio.common.dto.CompetenciaRequest;
import pe.edu.pucp.colegio.common.dto.CompetenciaResponse;
import pe.edu.pucp.colegio.common.model.Area;
import pe.edu.pucp.colegio.common.model.Competencia;
import pe.edu.pucp.colegio.common.repository.AreaRepository;
import pe.edu.pucp.colegio.common.repository.CompetenciaRepository;

@Service
@Transactional
public class CatalogoService {

  private final AreaRepository areas;
  private final CompetenciaRepository competencias;
  private final EntityManager entityManager;

  public CatalogoService(
      AreaRepository areas, CompetenciaRepository competencias, EntityManager entityManager) {
    this.areas = areas;
    this.competencias = competencias;
    this.entityManager = entityManager;
  }

  @Transactional(readOnly = true)
  public List<CatalogoAreaResponse> listarAreas() {
    return areas.findAll().stream()
        .sorted(Comparator.comparing(area -> normalizar(area.getNombre())))
        .map(this::toCatalogoResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public AreaResponse obtenerArea(String identificador) {
    return toResponse(buscarArea(identificador, false));
  }

  public CatalogoAreaResponse crearArea(AreaCatalogoCreateRequest request) {
    validarNombreAreaUnico(request.nombre(), null);
    boolean crearConCompetencias = request.competencias() != null;

    if (crearConCompetencias && request.competencias().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "El área debe tener al menos una competencia");
    }
    if (!crearConCompetencias && (request.codigo() == null || request.codigo().isBlank())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "El código es obligatorio para crear un área sin competencias anidadas");
    }

    String codigo = crearConCompetencias ? nuevoCodigo("AREA") : request.codigo().trim();
    if (areas.existsById(codigo)) {
      throw conflict("Ya existe un área con ese código");
    }

    Area area = new Area();
    area.setCodigo(codigo);
    area.setNombre(request.nombre().trim());
    area.setDescripcion(request.descripcion());
    area.setActivo(true);
    area = areas.save(area);
    entityManager.flush();
    entityManager.refresh(area);

    if (crearConCompetencias) {
      validarNombresCompetenciasNuevas(request.competencias());
      for (CompetenciaNuevaRequest solicitud : request.competencias()) {
        Competencia competencia = nuevaCompetencia(solicitud.nombre(), solicitud.descripcion());
        competencia = competencias.save(competencia);
        entityManager.flush();
        entityManager.refresh(competencia);
        competencia.getAreas().add(area);
        area.getCompetencias().add(competencia);
      }
      areas.save(area);
    } else if (request.competenciaCodigos() != null) {
      aplicarCompetencias(area, request.competenciaCodigos());
      areas.save(area);
    }

    entityManager.flush();
    return toCatalogoResponse(area);
  }

  public CatalogoAreaResponse actualizarArea(String identificador, AreaRequest request) {
    Area area = buscarArea(identificador, false);
    if (request.codigo() != null && !request.codigo().equals(area.getCodigo())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El código del cuerpo debe coincidir con el de la URL");
    }
    validarNombreAreaUnico(request.nombre(), area.getIdCatalogo());
    area.setNombre(request.nombre().trim());
    area.setDescripcion(request.descripcion());
    if (request.competenciaCodigos() != null) {
      aplicarCompetencias(area, request.competenciaCodigos());
    }
    return toCatalogoResponse(areas.save(area));
  }

  public CatalogoAreaResponse cambiarEstadoArea(Integer id, CambioEstadoCatalogoRequest request) {
    Area area =
        areas
            .findByIdCatalogo(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Área no encontrada"));
    area.setActivo(request.activo());
    areas.save(area);
    return toCatalogoResponse(area);
  }

  /** Mantiene la ruta DELETE existente como baja lógica para conservar el historial curricular. */
  public void eliminarArea(String identificador) {
    Area area = buscarArea(identificador, false);
    area.setActivo(false);
    areas.save(area);
  }

  @Transactional(readOnly = true)
  public CompetenciaResponse obtenerCompetencia(String identificador) {
    return toResponse(buscarCompetencia(identificador, false));
  }

  public CompetenciaResponse crearCompetencia(CompetenciaRequest request) {
    if (request.codigo() == null || request.codigo().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código es obligatorio");
    }
    if (competencias.existsById(request.codigo())) {
      throw conflict("Ya existe una competencia con ese código");
    }
    Competencia competencia = new Competencia();
    competencia.setCodigo(request.codigo());
    competencia.setNombre(request.nombre().trim());
    competencia.setDescripcion(request.descripcion());
    competencia.setActivo(true);
    competencia = competencias.save(competencia);
    entityManager.flush();
    entityManager.refresh(competencia);
    if (request.areaCodigos() != null) {
      aplicarAreas(competencia, request.areaCodigos());
      competencia = competencias.save(competencia);
    }
    return toResponse(competencia);
  }

  public CatalogoCompetenciaResponse crearCompetenciaEnArea(
      Integer areaId, CompetenciaNuevaRequest request) {
    Area area =
        areas
            .findByIdCatalogo(areaId)
            .filter(actual -> Boolean.TRUE.equals(actual.getActivo()))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Área no encontrada"));
    validarNombreCompetenciaUnico(request.nombre(), area, null);

    Competencia competencia = nuevaCompetencia(request.nombre(), request.descripcion());
    competencia = competencias.save(competencia);
    entityManager.flush();
    entityManager.refresh(competencia);
    competencia.getAreas().add(area);
    area.getCompetencias().add(competencia);
    areas.save(area);
    return toCatalogoCompetencia(competencia, area);
  }

  public CompetenciaResponse actualizarCompetencia(
      String identificador, CompetenciaRequest request) {
    Competencia competencia = buscarCompetencia(identificador, false);
    if (request.codigo() != null && !request.codigo().equals(competencia.getCodigo())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El código del cuerpo debe coincidir con el de la URL");
    }
    validarNombreCompetenciaEnAreas(request.nombre(), competencia);
    competencia.setNombre(request.nombre().trim());
    competencia.setDescripcion(request.descripcion());
    if (request.areaCodigos() != null) {
      aplicarAreas(competencia, request.areaCodigos());
    }
    return toResponse(competencias.save(competencia));
  }

  public CatalogoCompetenciaResponse cambiarEstadoCompetencia(
      Integer id, CambioEstadoCatalogoRequest request) {
    Competencia competencia =
        competencias
            .findByIdCatalogo(id)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Competencia no encontrada"));
    competencia.setActivo(request.activo());
    competencias.save(competencia);
    return toCatalogoCompetencia(
        competencia, competencia.getAreas().stream().findFirst().orElse(null));
  }

  public void eliminarCompetencia(String identificador) {
    Competencia competencia = buscarCompetencia(identificador, false);
    // DELETE conserva el catálogo y sus vínculos históricos mediante una baja lógica.
    competencia.setActivo(false);
    competencias.save(competencia);
  }

  private Competencia nuevaCompetencia(String nombre, String descripcion) {
    Competencia competencia = new Competencia();
    competencia.setCodigo(nuevoCodigo("COMP"));
    competencia.setNombre(nombre.trim());
    competencia.setDescripcion(descripcion);
    competencia.setActivo(true);
    return competencia;
  }

  private void aplicarCompetencias(Area area, Set<String> codigos) {
    desvincular(area);
    for (Competencia competencia : buscarCompetencias(codigos)) {
      if (!Boolean.TRUE.equals(competencia.getActivo())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "No se puede asociar una competencia inactiva");
      }
      area.getCompetencias().add(competencia);
      competencia.getAreas().add(area);
    }
  }

  private void aplicarAreas(Competencia competencia, Set<String> codigos) {
    for (Area area : new LinkedHashSet<>(competencia.getAreas())) {
      area.getCompetencias().remove(competencia);
    }
    competencia.getAreas().clear();
    for (Area area : buscarAreas(codigos)) {
      if (!Boolean.TRUE.equals(area.getActivo())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "No se puede asociar un área inactiva");
      }
      area.getCompetencias().add(competencia);
      competencia.getAreas().add(area);
    }
  }

  private void validarNombreAreaUnico(String nombre, Integer idExcluido) {
    String normalizado = normalizar(nombre);
    boolean duplicado =
        areas.findAll().stream()
            .anyMatch(
                area ->
                    !area.getIdCatalogo().equals(idExcluido)
                        && normalizar(area.getNombre()).equals(normalizado));
    if (duplicado) {
      throw conflict("Ya existe un área con ese nombre");
    }
  }

  private void validarNombresCompetenciasNuevas(List<CompetenciaNuevaRequest> solicitudes) {
    Set<String> nombres = new LinkedHashSet<>();
    for (CompetenciaNuevaRequest solicitud : solicitudes) {
      if (!nombres.add(normalizar(solicitud.nombre()))) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "No se puede repetir el nombre de una competencia en el área");
      }
    }
  }

  private void validarNombreCompetenciaUnico(String nombre, Area area, Integer idExcluido) {
    String normalizado = normalizar(nombre);
    boolean duplicado =
        area.getCompetencias().stream()
            .anyMatch(
                actual ->
                    !actual.getIdCatalogo().equals(idExcluido)
                        && normalizar(actual.getNombre()).equals(normalizado));
    if (duplicado) {
      throw conflict("Ya existe una competencia con ese nombre en el área");
    }
  }

  private void validarNombreCompetenciaEnAreas(String nombre, Competencia competencia) {
    for (Area area : competencia.getAreas()) {
      validarNombreCompetenciaUnico(nombre, area, competencia.getIdCatalogo());
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
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Uno o más códigos de competencia no existen");
    }
    return new LinkedHashSet<>(encontradas);
  }

  private Set<Area> buscarAreas(Set<String> codigos) {
    if (codigos == null || codigos.isEmpty()) return new LinkedHashSet<>();
    List<Area> encontradas = areas.findAllById(codigos);
    if (encontradas.size() != codigos.size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Uno o más códigos de área no existen");
    }
    return new LinkedHashSet<>(encontradas);
  }

  private Area buscarArea(String identificador, boolean incluirInactiva) {
    Area area =
        buscarPorIdentificador(
            identificador,
            areas.findByIdCatalogo(parsearId(identificador)).orElse(null),
            areas.findById(identificador).orElse(null));
    if (area == null || (!incluirInactiva && !Boolean.TRUE.equals(area.getActivo()))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Área no encontrada");
    }
    return area;
  }

  private Competencia buscarCompetencia(String identificador, boolean incluirInactiva) {
    Competencia competencia =
        buscarPorIdentificador(
            identificador,
            competencias.findByIdCatalogo(parsearId(identificador)).orElse(null),
            competencias.findById(identificador).orElse(null));
    if (competencia == null
        || (!incluirInactiva && !Boolean.TRUE.equals(competencia.getActivo()))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competencia no encontrada");
    }
    return competencia;
  }

  private <T> T buscarPorIdentificador(String identificador, T porId, T porCodigo) {
    return porId != null ? porId : porCodigo;
  }

  private Integer parsearId(String identificador) {
    try {
      return Integer.valueOf(identificador);
    } catch (NumberFormatException e) {
      return -1;
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
    Area primeraArea = competencia.getAreas().stream().findFirst().orElse(null);
    return new CompetenciaResponse(
        competencia.getCodigo(),
        competencia.getNombre(),
        competencia.getDescripcion(),
        competencia.getAreas().stream()
            .filter(areaAsociada -> Boolean.TRUE.equals(areaAsociada.getActivo()))
            .map(Area::getCodigo)
            .collect(Collectors.toCollection(LinkedHashSet::new)),
        competencia.getIdCatalogo(),
        primeraArea == null ? null : primeraArea.getIdCatalogo(),
        competencia.getActivo(),
        tieneNotasRegistradas(competencia));
  }

  private CatalogoAreaResponse toCatalogoResponse(Area area) {
    List<CatalogoCompetenciaResponse> listaCompetencias =
        area.getCompetencias().stream()
            .sorted(Comparator.comparing(comp -> normalizar(comp.getNombre())))
            .map(comp -> toCatalogoCompetencia(comp, area))
            .toList();
    return new CatalogoAreaResponse(
        area.getIdCatalogo(),
        area.getCodigo(),
        area.getNombre(),
        area.getDescripcion(),
        area.getActivo(),
        listaCompetencias.stream()
            .map(CatalogoCompetenciaResponse::codigo)
            .collect(Collectors.toCollection(LinkedHashSet::new)),
        listaCompetencias);
  }

  private CatalogoCompetenciaResponse toCatalogoCompetencia(Competencia competencia, Area area) {
    return new CatalogoCompetenciaResponse(
        competencia.getIdCatalogo(),
        area == null ? null : area.getIdCatalogo(),
        competencia.getCodigo(),
        competencia.getNombre(),
        competencia.getDescripcion(),
        competencia.getActivo(),
        tieneNotasRegistradas(competencia),
        competencia.getAreas().stream()
            .filter(areaAsociada -> Boolean.TRUE.equals(areaAsociada.getActivo()))
            .map(Area::getCodigo)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
  }

  private boolean tieneNotasRegistradas(Competencia competencia) {
    // El módulo de calificaciones aún no tiene entidad ni tabla de notas en este backend.
    return false;
  }

  private String nuevoCodigo(String prefijo) {
    return prefijo + "-" + UUID.randomUUID();
  }

  private String normalizar(String texto) {
    return Normalizer.normalize(texto == null ? "" : texto.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .replaceAll("\\s+", " ")
        .toLowerCase(Locale.ROOT);
  }

  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
