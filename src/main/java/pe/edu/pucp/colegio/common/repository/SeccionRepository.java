package pe.edu.pucp.colegio.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Seccion;

public interface SeccionRepository extends JpaRepository<Seccion, Integer> {}
