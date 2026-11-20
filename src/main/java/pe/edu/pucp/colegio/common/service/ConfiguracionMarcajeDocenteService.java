package pe.edu.pucp.colegio.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.pucp.colegio.common.dto.ConfiguracionMarcajeDocenteRequest;
import pe.edu.pucp.colegio.common.dto.ConfiguracionMarcajeDocenteResponse;
import pe.edu.pucp.colegio.common.model.ConfiguracionMarcajeDocente;
import pe.edu.pucp.colegio.common.repository.ConfiguracionMarcajeDocenteRepository;

/** Lee y actualiza la única configuración global del marcado docente. */
@Service
public class ConfiguracionMarcajeDocenteService {

  private static final int ID_CONFIGURACION = 1;
  private final ConfiguracionMarcajeDocenteRepository configuraciones;

  public ConfiguracionMarcajeDocenteService(ConfiguracionMarcajeDocenteRepository configuraciones) {
    this.configuraciones = configuraciones;
  }

  @Transactional(readOnly = true)
  public ConfiguracionMarcajeDocenteResponse obtener() {
    return respuesta(buscarConfiguracion());
  }

  @Transactional
  public ConfiguracionMarcajeDocenteResponse actualizar(
      ConfiguracionMarcajeDocenteRequest request) {
    if (request.toleranciaMinutos() > request.ventanaDespuesMinutos()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "La tolerancia no puede superar el tiempo disponible para marcar después del inicio de jornada");
    }
    ConfiguracionMarcajeDocente configuracion = buscarConfiguracion();
    configuracion.setToleranciaMinutos(request.toleranciaMinutos());
    configuracion.setVentanaAntesMinutos(request.ventanaAntesMinutos());
    configuracion.setVentanaDespuesMinutos(request.ventanaDespuesMinutos());
    return respuesta(configuraciones.save(configuracion));
  }

  private ConfiguracionMarcajeDocente buscarConfiguracion() {
    return configuraciones
        .findById(ID_CONFIGURACION)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se encontró la configuración global de marcado docente"));
  }

  private ConfiguracionMarcajeDocenteResponse respuesta(ConfiguracionMarcajeDocente configuracion) {
    return new ConfiguracionMarcajeDocenteResponse(
        configuracion.getToleranciaMinutos(),
        configuracion.getVentanaAntesMinutos(),
        configuracion.getVentanaDespuesMinutos());
  }
}
