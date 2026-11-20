package pe.edu.pucp.colegio.common.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.colegio.common.dto.ActualizarUmbralRiesgoRequest;
import pe.edu.pucp.colegio.common.dto.UmbralRiesgoResponse;
import pe.edu.pucp.colegio.common.model.ConfiguracionRiesgoAcademico;
import pe.edu.pucp.colegio.common.repository.ConfiguracionRiesgoAcademicoRepository;

/** Administra el umbral global usado para identificar riesgo académico. */
@Service
public class ConfiguracionRiesgoAcademicoService {

  private static final int ID_CONFIGURACION = 1;

  private final ConfiguracionRiesgoAcademicoRepository configuraciones;

  public ConfiguracionRiesgoAcademicoService(
      ConfiguracionRiesgoAcademicoRepository configuraciones) {
    this.configuraciones = configuraciones;
  }

  /** Devuelve el umbral vigente. */
  @Transactional(readOnly = true)
  public UmbralRiesgoResponse obtener() {
    return new UmbralRiesgoResponse(buscarConfiguracion().getUmbral());
  }

  /** Actualiza el umbral persistido. */
  @Transactional
  public UmbralRiesgoResponse actualizar(ActualizarUmbralRiesgoRequest request) {
    ConfiguracionRiesgoAcademico configuracion = buscarConfiguracion();
    configuracion.setUmbral(request.umbral());
    return new UmbralRiesgoResponse(configuraciones.save(configuracion).getUmbral());
  }

  private ConfiguracionRiesgoAcademico buscarConfiguracion() {
    return configuraciones.findById(ID_CONFIGURACION).orElseThrow();
  }
}
