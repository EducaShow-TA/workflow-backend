package pe.edu.pucp.colegio.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Grado;

public interface GradoRepository extends JpaRepository<Grado, Integer> {}
