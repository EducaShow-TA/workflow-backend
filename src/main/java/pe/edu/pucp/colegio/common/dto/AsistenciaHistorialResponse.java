package pe.edu.pucp.colegio.common.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import pe.edu.pucp.colegio.common.model.EstadoAsistencia;

public record AsistenciaHistorialResponse(
    Integer asistenciaId,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    Integer seccionId,
    String seccionCodigo,
    String areaCodigo,
    String areaNombre,
    String docenteNombre,
    EstadoAsistencia estado,
    String motivoJustificacion,
    String documentoNombre,
    String documentoUrl) {}
