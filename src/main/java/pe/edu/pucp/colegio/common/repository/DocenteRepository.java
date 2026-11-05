package pe.edu.pucp.colegio.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.pucp.colegio.common.model.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Integer> {}
