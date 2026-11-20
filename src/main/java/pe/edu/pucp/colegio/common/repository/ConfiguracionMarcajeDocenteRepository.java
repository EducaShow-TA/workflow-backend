package pe.edu.pucp.colegio.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.ConfiguracionMarcajeDocente;

public interface ConfiguracionMarcajeDocenteRepository
    extends JpaRepository<ConfiguracionMarcajeDocente, Integer> {}
