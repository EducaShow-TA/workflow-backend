package pe.edu.pucp.colegio.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.HorarioSeccion;

public interface HorarioSeccionRepository extends JpaRepository<HorarioSeccion, Integer> {

    Optional<HorarioSeccion> findBySeccionIdAndAnio(Integer seccionId, Integer anio);

    boolean existsBySeccionIdAndAnio(Integer seccionId, Integer anio);
}
