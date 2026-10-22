package pe.edu.pucp.colegio.common.service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.BloqueHorarioResponse;
import pe.edu.pucp.colegio.common.dto.CambioEstadoPlantillaRequest;
import pe.edu.pucp.colegio.common.dto.PlantillaHorarioRequest;
import pe.edu.pucp.colegio.common.dto.PlantillaHorarioResponse;
import pe.edu.pucp.colegio.common.dto.RecreoHorarioRequest;
import pe.edu.pucp.colegio.common.exception.BusinessException;
import pe.edu.pucp.colegio.common.exception.ConflictException;
import pe.edu.pucp.colegio.common.model.BloqueHorario;
import pe.edu.pucp.colegio.common.model.PlantillaHorario;
import pe.edu.pucp.colegio.common.model.TipoBloqueHorario;
import pe.edu.pucp.colegio.common.repository.BloqueHorarioRepository;
import pe.edu.pucp.colegio.common.repository.PlantillaHorarioRepository;

@Service
public class PlantillaHorarioService {

  private final PlantillaHorarioRepository plantillas;
  private final BloqueHorarioRepository bloques;

  public PlantillaHorarioService(
      PlantillaHorarioRepository plantillas, BloqueHorarioRepository bloques) {
    this.plantillas = plantillas;
    this.bloques = bloques;
  }

  @Transactional(readOnly = true)
  public List<PlantillaHorarioResponse> listar() {
    return plantillas.findAllByOrderByTurnoAscNombreAsc().stream().map(this::respuesta).toList();
  }

  @Transactional(readOnly = true)
  public PlantillaHorarioResponse obtener(Integer id) {
    return respuesta(buscar(id));
  }

  @Transactional
  public PlantillaHorarioResponse crear(PlantillaHorarioRequest request) {
    validarNombreDisponible(request, null);
    PlantillaHorario plantilla = new PlantillaHorario();
    aplicar(plantilla, request);
    return respuesta(plantillas.save(plantilla));
  }

  @Transactional
  public PlantillaHorarioResponse actualizar(Integer id, PlantillaHorarioRequest request) {
    PlantillaHorario plantilla = buscar(id);
    validarNombreDisponible(request, id);
    List<BloqueHorario> nuevosBloques = generarBloques(request);

    plantilla.reemplazarBloques(List.of());
    bloques.deleteAllByPlantillaId(id);
    bloques.flush();
    aplicarDatos(plantilla, request);
    plantilla.reemplazarBloques(nuevosBloques);
    return respuesta(plantillas.saveAndFlush(plantilla));
  }

  @Transactional
  public PlantillaHorarioResponse cambiarEstado(Integer id, CambioEstadoPlantillaRequest request) {
    PlantillaHorario plantilla = buscar(id);
    if (Boolean.TRUE.equals(request.activo())) {
      validarNombreDisponible(
          new PlantillaHorarioRequest(
              plantilla.getNombre(),
              plantilla.getTurno(),
              plantilla.getHoraInicio(),
              plantilla.getHoraFin(),
              plantilla.getDuracionHoraPedagogicaMinutos(),
              plantilla.getCantidadBloquesPedagogicos(),
              List.of()),
          id);
    }
    plantilla.setActive(request.activo());
    return respuesta(plantillas.save(plantilla));
  }

  @Transactional
  public void desactivar(Integer id) {
    PlantillaHorario plantilla = buscar(id);
    plantilla.setActive(false);
    plantillas.save(plantilla);
  }

  private void aplicar(PlantillaHorario plantilla, PlantillaHorarioRequest request) {
    aplicarDatos(plantilla, request);
    plantilla.reemplazarBloques(generarBloques(request));
  }

  private void aplicarDatos(PlantillaHorario plantilla, PlantillaHorarioRequest request) {
    plantilla.setNombre(request.nombre().trim());
    plantilla.setTurno(request.turno());
    plantilla.setHoraInicio(request.horaInicio());
    plantilla.setHoraFin(request.horaFin());
    plantilla.setDuracionHoraPedagogicaMinutos(request.duracionHoraPedagogicaMinutos());
    plantilla.setCantidadBloquesPedagogicos(request.cantidadBloquesPedagogicos());
  }

  private List<BloqueHorario> generarBloques(PlantillaHorarioRequest request) {
    if (!request.horaInicio().isBefore(request.horaFin())) {
      throw new BusinessException("La hora de inicio debe ser anterior a la hora de fin");
    }
    if (request.turno() == pe.edu.pucp.colegio.common.model.TurnoEscolar.MANANA
        && !request.horaInicio().isBefore(LocalTime.NOON)) {
      throw new BusinessException("El turno mañana debe iniciar antes de las 12:00");
    }
    if (request.turno() == pe.edu.pucp.colegio.common.model.TurnoEscolar.TARDE
        && request.horaInicio().isBefore(LocalTime.NOON)) {
      throw new BusinessException("El turno tarde debe iniciar desde las 12:00");
    }

    Map<Integer, Integer> recreosPorBloque = new TreeMap<>();
    long duracionTotal =
        (long) request.duracionHoraPedagogicaMinutos() * request.cantidadBloquesPedagogicos();
    for (RecreoHorarioRequest recreo : request.recreos()) {
      if (recreo.despuesDeBloque() > request.cantidadBloquesPedagogicos()) {
        throw new BusinessException(
            "El recreo debe ubicarse después de un bloque pedagógico existente");
      }
      if (recreosPorBloque.putIfAbsent(recreo.despuesDeBloque(), recreo.duracionMinutos())
          != null) {
        throw new BusinessException("No se permiten dos recreos después del mismo bloque");
      }
      duracionTotal += recreo.duracionMinutos();
    }
    long duracionTurno = ChronoUnit.MINUTES.between(request.horaInicio(), request.horaFin());
    if (duracionTotal > duracionTurno) {
      throw new BusinessException("Los bloques y recreos exceden la hora de fin del turno");
    }

    List<BloqueHorario> resultado = new ArrayList<>();
    LocalTime inicio = request.horaInicio();
    int orden = 1;

    for (int numeroBloque = 1;
        numeroBloque <= request.cantidadBloquesPedagogicos();
        numeroBloque++) {
      LocalTime fin = inicio.plusMinutes(request.duracionHoraPedagogicaMinutos());
      resultado.add(nuevoBloque(orden++, TipoBloqueHorario.PEDAGOGICO, inicio, fin));
      inicio = fin;

      Integer duracionRecreo = recreosPorBloque.get(numeroBloque);
      if (duracionRecreo != null) {
        fin = inicio.plusMinutes(duracionRecreo);
        resultado.add(nuevoBloque(orden++, TipoBloqueHorario.RECREO, inicio, fin));
        inicio = fin;
      }
    }

    if (inicio.isAfter(request.horaFin())) {
      throw new BusinessException("Los bloques y recreos exceden la hora de fin del turno");
    }
    return resultado;
  }

  private BloqueHorario nuevoBloque(
      Integer orden, TipoBloqueHorario tipo, LocalTime horaInicio, LocalTime horaFin) {
    BloqueHorario bloque = new BloqueHorario();
    bloque.setOrden(orden);
    bloque.setTipo(tipo);
    bloque.setHoraInicio(horaInicio);
    bloque.setHoraFin(horaFin);
    return bloque;
  }

  private void validarNombreDisponible(PlantillaHorarioRequest request, Integer id) {
    boolean duplicado =
        id == null
            ? plantillas.existsByTurnoAndNombreIgnoreCase(request.turno(), request.nombre().trim())
            : plantillas.existsByTurnoAndNombreIgnoreCaseAndIdNot(
                request.turno(), request.nombre().trim(), id);
    if (duplicado) {
      throw new ConflictException("Ya existe una plantilla con ese nombre para el turno");
    }
  }

  private PlantillaHorario buscar(Integer id) {
    return plantillas
        .findWithBloquesById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No existe la plantilla horaria"));
  }

  private PlantillaHorarioResponse respuesta(PlantillaHorario plantilla) {
    List<BloqueHorarioResponse> respuestas =
        plantilla.getBloques().stream()
            .map(
                bloque ->
                    new BloqueHorarioResponse(
                        bloque.getId(),
                        bloque.getOrden(),
                        bloque.getTipo(),
                        bloque.getHoraInicio(),
                        bloque.getHoraFin()))
            .toList();
    return new PlantillaHorarioResponse(
        plantilla.getId(),
        plantilla.getNombre(),
        plantilla.getTurno(),
        plantilla.getHoraInicio(),
        plantilla.getHoraFin(),
        plantilla.getDuracionHoraPedagogicaMinutos(),
        plantilla.getCantidadBloquesPedagogicos(),
        plantilla.getActive(),
        respuestas);
  }
}
