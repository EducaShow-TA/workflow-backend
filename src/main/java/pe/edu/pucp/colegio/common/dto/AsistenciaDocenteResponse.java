package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import pe.edu.pucp.colegio.common.model.EstadoAsistenciaDocente;

public record AsistenciaDocenteResponse(
    Integer id,
    Integer docenteId,
    String docenteNombre,
    LocalDate fecha,
    LocalTime horaEntrada,
    LocalTime horaSalida,
    EstadoAsistenciaDocente estado,
    String motivoAjuste) {}
